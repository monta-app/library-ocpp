package com.monta.library.ocpp.common.serialization

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.JsonNode

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
sealed class Message(
    /**
     * this is a unique identifier that will be used to match request and result.
     */
    open val uniqueId: String
) {
    data class Request(
        /**
         * this is a unique identifier that will be used to match request and result.
         */
        override val uniqueId: String,
        /**
         * the name of the remote procedure or action.
         * This will be a case-sensitive string containing the same value as the Action-field
         * in SOAP-based messages,without the preceding slash
         */
        val action: String,
        /**
         * Payload is a JSON object containing the arguments relevant to the Action.
         * If there is no payload JSON allows for two different notations: null or and empty object \{}.
         * Although it seems trivial we consider it good practice to only use the empty object statement.
         * Null usually represents something undefined, which is not the same as empty, and also \{} is shorter.
         */
        override val payload: JsonNode
    ) : Message(uniqueId), Payloadable {

        companion object {
            private const val MESSAGE_TYPE_ID: Int = 2
            private const val JSON_FORMAT = "[$MESSAGE_TYPE_ID,\"%s\",\"%s\",%s]"
        }

        override fun toJsonString(serializer: MessageSerializer): String {
            return JSON_FORMAT.format(uniqueId, action, serializer.serializePayload(payload))
        }
    }

    data class Response(

        /**
         * this is a unique identifier that will be used to match request and result.
         */
        override val uniqueId: String,
        /**
         * Payload is a JSON object containing the arguments relevant to the Action.
         * If there is no payload JSON allows for two different notations: null or and empty object \{}.
         * Although it seems trivial we consider it good practice to only use the empty object statement.
         * Null usually represents something undefined, which is not the same as empty, and also \{} is shorter.
         */
        override val payload: JsonNode
    ) : Message(uniqueId), Payloadable {

        companion object {
            private const val MESSAGE_TYPE_ID: Int = 3
            private const val JSON_FORMAT = "[$MESSAGE_TYPE_ID,\"%s\",%s]"
        }

        override fun toJsonString(serializer: MessageSerializer): String {
            return JSON_FORMAT.format(uniqueId, serializer.serializePayload(payload))
        }
    }

    data class Error(
        /**
         * This must be the exact same id that is in the call request
         * so that the recipient can match request and result.
         */
        override val uniqueId: String,
        /**
         * This field must contain a string from theErrorCode table below.
         */
        var errorCode: String,
        /**
         * Should be filled in if possible, otherwise a clear empty string “”
         */
        var errorDescription: String,
        /**
         * This JSON object describes error details in an undefined way.
         * If there are no error details you MUST fill in an empty object \{}
         */
        var errorDetails: String = "{}"
    ) : Message(uniqueId) {

        companion object {
            private const val MESSAGE_TYPE_ID: Int = 4
            private const val JSON_FORMAT = "[$MESSAGE_TYPE_ID,\"%s\",\"%s\",\"%s\",%s]"
        }

        override fun toJsonString(serializer: MessageSerializer): String {
            return JSON_FORMAT.format(uniqueId, errorCode, errorDescription, errorDetails)
        }
    }

    abstract fun toJsonString(serializer: MessageSerializer): String
}
