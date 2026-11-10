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
     * The frame had a message type the receiver must ignore (OCPP-J 2.1, part 4, section 4.1.3). No response must be sent.
     */
    data class Ignored<T>(
        val uniqueId: String,
        val messageTypeId: Int?
    ) : ParsingResult<T>()
}
