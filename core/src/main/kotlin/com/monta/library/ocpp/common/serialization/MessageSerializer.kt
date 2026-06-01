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
    OCPP_2
}

class MessageSerializer(
    private val serializationMode: SerializationMode,
    private val ocppErrorResponder: OcppErrorResponder
) {
    companion object {
        private const val TYPE_NUMBER_CALL = 2
        private const val TYPE_NUMBER_CALL_RESULT = 3
        private const val TYPE_NUMBER_CALL_ERROR = 4

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

                    SerializationMode.OCPP_2 -> {
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

    fun parseMessageId(json: String): Result<String> {
        return runCatching {
            val jsonNode = objectMapper.readTree(json)
            check(jsonNode.isArray)
            jsonNode.get(1).asText()
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
                uniqueId = "",
                messageErrorCode = ocppErrorResponder.getJsonFormatError(),
                throwable = exception
            )
        }
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

            else -> ParsingResult.Failure(
                uniqueId = this[1].asText(),
                messageErrorCode = ocppErrorResponder.getPropertyConstraintViolationError(),
                throwable = IllegalArgumentException("unknown message type messageType=$messageType")
            )
        }
    }

    private fun JsonNode.toOcppRequestMessage(): Message.Request {
        return Message.Request(
            uniqueId = this[1].asText(),
            action = this[2].asText(),
            payload = this[3]
        )
    }

    private fun JsonNode.toOcppResponseMessage(): Message.Response {
        return Message.Response(
            uniqueId = this[1].asText(),
            payload = this[2]
        )
    }

    private fun JsonNode.toOcppErrorMessage(): Message.Error {
        return Message.Error(
            uniqueId = this[1].asText(),
            errorCode = this[2].asText(),
            errorDescription = this[3].asText(),
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
