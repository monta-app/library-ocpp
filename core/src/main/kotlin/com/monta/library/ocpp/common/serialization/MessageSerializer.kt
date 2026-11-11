package com.monta.library.ocpp.common.serialization

import com.fasterxml.jackson.annotation.JsonInclude
import com.monta.library.ocpp.common.error.OcppErrorResponder
import org.slf4j.LoggerFactory
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.ser.std.StdSerializer
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class SerializationMode {
    OCPP_1_6,
    OCPP_2,

    /**
     * OCPP 2.1: same payload serialization as [OCPP_2], but the RPC framework also knows CALLRESULTERROR (5) and SEND (6)
     * and ignores frames with any other unknown message type (OCPP-J 2.1, part 4, section 4.1.3).
     */
    OCPP_2_1
}

class MessageSerializer(
    private val serializationMode: SerializationMode,
    private val ocppErrorResponder: OcppErrorResponder
) {
    companion object {
        private const val TYPE_NUMBER_CALL = 2
        private const val TYPE_NUMBER_CALL_RESULT = 3
        private const val TYPE_NUMBER_CALL_ERROR = 4
        private const val TYPE_NUMBER_CALL_RESULT_ERROR = 5
        private const val TYPE_NUMBER_SEND = 6

        /** OCPP-J 2.1 part 4, 4.2.3: "When also the MessageId cannot be read, the CALLERROR SHALL contain "-1"". */
        private const val UNREADABLE_MESSAGE_ID = "-1"

        @JvmStatic
        private val logger = LoggerFactory.getLogger(MessageSerializer::class.java)
    }

    // Jackson 3 mappers are immutable and configured through the builder. java.time support is
    // built into databind now, so we only register a SimpleModule for our custom ZonedDateTime
    // serializers instead of the old JavaTimeModule.
    private val objectMapper = jacksonMapperBuilder()
        .addModule(
            SimpleModule().apply {
                // this is a bit of a flaky way to differentiate between 1.6 and 2.0.1 serialization.
                // All 1.6 data classes use ZonedDateTime, 2.0.1 uses OffsetDateTime.
                // Some 1.6 charge points do not accept high resolution timestamps so we just truncate to seconds.
                // 2.0.1 specification is clear that at most millisecond resolution is allowed (part 2, section 2.1.3 Primitive Datatypes).
                when (serializationMode) {
                    SerializationMode.OCPP_1_6 -> {
                        addSerializer(ZonedDateTime::class.java, Ocpp16ZonedDateSerializer())
                    }

                    SerializationMode.OCPP_2,
                    SerializationMode.OCPP_2_1 -> {
                        addSerializer(ZonedDateTime::class.java, Ocpp2ZonedDateSerializer())
                    }
                }
            }
        )
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .findAndAddModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()

    private val supportsOcpp21Frames = serializationMode == SerializationMode.OCPP_2_1

    fun parseMessageId(json: String): Result<String> {
        return runCatching {
            val jsonNode = objectMapper.readTree(json)
            check(jsonNode.isArray)
            jsonNode.get(1).asString()
        }
    }

    fun parse(
        json: String
    ): ParsingResult<Message> {
        return try {
            val jsonNode = objectMapper.readTree(json)

            check(jsonNode.isArray)

            jsonNode.toOcppMessage()
        } catch (exception: Exception) {
            logger.warn("failed to deserialize message", exception)
            ParsingResult.Failure(
                uniqueId = if (supportsOcpp21Frames) readableMessageId(json) ?: UNREADABLE_MESSAGE_ID else "",
                messageErrorCode = ocppErrorResponder.getJsonFormatError(),
                throwable = exception
            )
        }
    }

    /** The frame's message id if it can be read, otherwise null. */
    private fun readableMessageId(json: String): String? {
        val node = runCatching { objectMapper.readTree(json) }.getOrNull() ?: return null
        val id = node.takeIf { it.isArray }?.get(1)
        return if (id != null && id.isString) id.asString() else null
    }

    fun <T> parseDataTransferExtension(
        json: String?,
        clazz: Class<T>
    ): T {
        return objectMapper.readValue(json, clazz)
    }

    fun <T, R> deserializePayload(
        ocppMessage: R,
        clazz: Class<T>
    ): ParsingResult<T> where R : Message, R : Payloadable {
        return try {
            ParsingResult.Success(
                value = deserializePayload(ocppMessage.payload, clazz)
            )
        } catch (exception: Exception) {
            logger.warn("failed to deserialize payload", exception)
            ParsingResult.Failure(
                uniqueId = ocppMessage.uniqueId,
                messageErrorCode = ocppErrorResponder.getJsonFormatError(),
                throwable = exception
            )
        }
    }

    fun getEmptyPayload(): ObjectNode {
        return objectMapper.createObjectNode()
    }

    fun toPayload(value: Any): JsonNode {
        return objectMapper.valueToTree(value)
    }

    fun toPayloadString(value: Any): String {
        return serializePayload(toPayload(value))
    }

    fun serializePayload(payload: JsonNode?): String {
        return if (payload != null) objectMapper.writeValueAsString(payload) else "{}"
    }

    private fun JsonNode.toOcppMessage(): ParsingResult<Message> {
        return when (val messageType = this[0]?.asInt()) {
            TYPE_NUMBER_CALL -> ParsingResult.Success(
                value = this.toOcppRequestMessage()
            )

            TYPE_NUMBER_CALL_RESULT -> ParsingResult.Success(
                value = this.toOcppResponseMessage()
            )

            TYPE_NUMBER_CALL_ERROR -> ParsingResult.Success(
                value = this.toOcppErrorMessage()
            )

            TYPE_NUMBER_CALL_RESULT_ERROR -> if (supportsOcpp21Frames) {
                this.toOcppCallResultErrorMessage()
                    ?.let { ParsingResult.Success<Message>(value = it) }
                    ?: this.ignored(messageType, "CALLRESULTERROR frame cannot be read")
            } else {
                this.unknownMessageType(messageType)
            }

            TYPE_NUMBER_SEND -> if (supportsOcpp21Frames) {
                this.toOcppSendMessage()
                    ?.let { ParsingResult.Success<Message>(value = it) }
                    ?: this.ignored(messageType, "SEND frame cannot be read")
            } else {
                this.unknownMessageType(messageType)
            }

            else -> this.unknownMessageType(messageType)
        }
    }

    private fun JsonNode.ignored(messageType: Int?, reason: String): ParsingResult<Message> {
        val id = this[1]
        return ParsingResult.Ignored(
            uniqueId = if (id != null && id.isString) id.asString() else "",
            messageTypeId = messageType,
            reason = reason
        )
    }

    private fun JsonNode.unknownMessageType(messageType: Int?): ParsingResult<Message> {
        if (supportsOcpp21Frames) {
            // OCPP 2.1: "When a system receives a message with a Message Type Number not in this list, it SHALL ignore the message"
            return this.ignored(messageType, "unknown message type $messageType")
        }
        return ParsingResult.Failure(
            uniqueId = this[1].asString(),
            messageErrorCode = ocppErrorResponder.getPropertyConstraintViolationError(),
            throwable = IllegalArgumentException("unknown message type messageType=$messageType")
        )
    }

    private fun JsonNode.toOcppRequestMessage(): Message.Request {
        return Message.Request(
            uniqueId = this[1].asString(),
            action = this[2].asString(),
            payload = this[3]
        )
    }

    private fun JsonNode.toOcppResponseMessage(): Message.Response {
        return Message.Response(
            uniqueId = this[1].asString(),
            payload = this[2]
        )
    }

    /** Null when the frame cannot be read: `[5, "id", "errorCode", "errorDescription", {errorDetails}]`. */
    private fun JsonNode.toOcppCallResultErrorMessage(): Message.CallResultError? {
        val id = this[1]
        val code = this[2]
        val description = this[3]
        if (id == null || !id.isString || code == null || !code.isString || description == null || !description.isString) return null
        return Message.CallResultError(
            uniqueId = id.asString(),
            errorCode = code.asString(),
            errorDescription = description.asString(),
            errorDetails = this[4]?.toString() ?: "{}"
        )
    }

    /** Null when the frame cannot be read: `[6, "id", "Action", {payload}]`. */
    private fun JsonNode.toOcppSendMessage(): Message.Send? {
        val id = this[1]
        val action = this[2]
        val payload = this[3]
        if (id == null || !id.isString || action == null || !action.isString || payload == null || !payload.isObject) return null
        return Message.Send(
            uniqueId = id.asString(),
            action = action.asString(),
            payload = payload
        )
    }

    /**
     * Writes an OCPP-J frame `[messageTypeId, "uniqueId", ...elements]` with proper JSON escaping.
     * Elements may be [String]s, [Int]s or [JsonNode]s.
     */
    internal fun writeFrame(
        messageTypeId: Int,
        uniqueId: String,
        vararg elements: Any
    ): String {
        val frame = objectMapper.createArrayNode()
        frame.add(messageTypeId)
        frame.add(uniqueId)
        elements.forEach { element ->
            when (element) {
                is JsonNode -> frame.add(element)
                is String -> frame.add(element)
                is Int -> frame.add(element)
                else -> frame.add(objectMapper.valueToTree<JsonNode>(element))
            }
        }
        return objectMapper.writeValueAsString(frame)
    }

    /**
     * Parses the raw errorDetails of a CALLERROR / CALLRESULTERROR, falling back to an empty object if it is not a JSON object.
     */
    internal fun parseErrorDetails(errorDetails: String): JsonNode {
        val parsed = runCatching { objectMapper.readTree(errorDetails) }.getOrNull()
        return if (parsed != null && parsed.isObject) parsed else objectMapper.createObjectNode()
    }

    private fun JsonNode.toOcppErrorMessage(): Message.Error {
        return Message.Error(
            uniqueId = this[1].asString(),
            errorCode = this[2].asString(),
            errorDescription = this[3].asString(),
            errorDetails = this[4].toString()
        )
    }

    private fun <T> deserializePayload(
        jsonNode: JsonNode,
        clazz: Class<T>
    ): T {
        return objectMapper.treeToValue(jsonNode, clazz)
    }

    class Ocpp16ZonedDateSerializer : StdSerializer<ZonedDateTime>(ZonedDateTime::class.java) {
        companion object {
            private val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        }

        override fun serialize(
            value: ZonedDateTime?,
            gen: JsonGenerator?,
            ctxt: SerializationContext?
        ) {
            gen?.writeString(FORMAT.format(value))
        }
    }

    class Ocpp2ZonedDateSerializer : StdSerializer<ZonedDateTime>(ZonedDateTime::class.java) {
        companion object {
            private val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        }

        override fun serialize(
            value: ZonedDateTime?,
            gen: JsonGenerator?,
            ctxt: SerializationContext?
        ) {
            gen?.writeString(FORMAT.format(value))
        }
    }
}
