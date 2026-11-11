package com.monta.library.ocpp.common.serialization

import com.monta.library.ocpp.common.error.OcppErrorCode

sealed class ParsingResult<T> {
    data class Success<T>(
        val value: T
    ) : ParsingResult<T>()

    data class Failure<T>(
        val uniqueId: String,
        val messageErrorCode: OcppErrorCode,
        val throwable: Throwable
    ) : ParsingResult<T>() {
        fun toError(): Message.Error {
            return Message.Error(
                uniqueId = this.uniqueId,
                errorCode = this.messageErrorCode.getName(),
                errorDescription = this.messageErrorCode.getMessage()
            )
        }
    }

    /**
     * OCPP 2.1 only: a frame the receiver must not answer. Either its message type is unknown (OCPP-J 2.1, part 4,
     * 4.1.3 and 4.4), or it is a SEND or CALLRESULTERROR that cannot be read (part 4, 4.2.4: a SEND is never answered;
     * a CALLRESULTERROR is itself an answer).
     */
    data class Ignored<T>(
        val uniqueId: String,
        val messageTypeId: Int?,
        val reason: String
    ) : ParsingResult<T>()
}
