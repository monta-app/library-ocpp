package com.monta.library.ocpp.v21.error

import com.monta.library.ocpp.common.error.OcppErrorCode

enum class MessageErrorCodeV21(
    private val message: String
) : OcppErrorCode {
    FormatViolation("Payload for Action is syntactically incorrect"),
    GenericError("Any other error not covered by the more specific error codes in this table"),
    InternalError("An internal error occurred and the receiver was not able to process the requested Action successfully"),

    /**
     * Deprecated in OCPP 2.1 (OCPP-J part 4 errata 2026-02): a message with an unknown message type is ignored, not
     * answered. Kept so that a CALLERROR carrying this code can still be read.
     */
    MessageTypeNotSupported("(deprecated) A message with a Message Type Number that is not supported by this implementation."),

    NotImplemented("Requested Action is not known by receiver"),
    NotSupported("Requested Action is recognized but not supported by the receiver"),
    OccurrenceConstraintViolation("Payload for Action is syntactically correct but at least one of the fields violates occurrence constraints"),
    PropertyConstraintViolation("Payload is syntactically correct but at least one field contains an invalid value"),
    ProtocolError("Payload for Action is not conform the PDU structure"),
    RpcFrameworkError("Content of the call is not a valid RPC Request, for example: MessageId could not be read."),
    SecurityError("During the processing of Action a security issue occurred preventing receiver from completing the Action successfully"),
    TypeConstraintViolation("Payload for Action is syntactically correct but at least one of the fields violates data type constraints (e.g. 'somestring': 12)");

    override fun getName(): String {
        return name
    }

    override fun getMessage(): String {
        return message
    }
}
