package com.monta.library.ocpp.common.transport

import com.monta.library.ocpp.common.error.OcppErrorCode
import com.monta.library.ocpp.common.error.OcppErrorResponder
import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import com.monta.library.ocpp.common.profile.OcppUnconfirmedRequest
import com.monta.library.ocpp.common.profile.ProfileDispatcher
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.Payloadable
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.common.session.OcppSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.MDC
import java.util.UUID
import kotlin.time.Duration

/** OCPP-J: ErrorDescription is string[255]. */
private const val MAX_ERROR_DESCRIPTION_LENGTH = 255

abstract class OcppMessageInterpreter(
    serializationMode: SerializationMode,
    private val ocppErrorResponder: OcppErrorResponder,
    private val logger: Logger,
    private val settings: OcppSettings,
    profiles: Set<ProfileDispatcher>
) {

    protected val messageSerializer = MessageSerializer(serializationMode, ocppErrorResponder)
    private val ocpp21 = serializationMode == SerializationMode.OCPP_2_1
    private val completeDeferrableRepository = CompleteDeferrableRepository<DeferredCache, OcppConfirmation>()

    private val actionToProfile = run {
        val pairs = profiles.flatMap { profile ->
            profile.featureList.map { feature ->
                feature.name to (profile to feature)
            }
        }
        require(pairs.distinctBy { it.first }.size == pairs.size) { "Duplicate action names in profiles: ${pairs.map { it.first }}" }
        pairs.toMap()
    }

    private val requestTypeToFeature = run {
        val pairs = profiles.flatMap { profile ->
            profile.featureList.map { feature ->
                feature.requestType to feature
            }
        }
        require(pairs.distinctBy { it.first }.size == pairs.size) { "Duplicate request types in profiles: ${pairs.map { it.first }}" }
        pairs.toMap()
    }

    suspend fun receiveMessage(
        ocppSessionInfo: OcppSession.Info,
        jsonMessage: String
    ) {
        when (val result = messageSerializer.parse(jsonMessage)) {
            is ParsingResult.Failure -> {
                sendMessage(
                    ocppSessionInfo = ocppSessionInfo,
                    message = Message.Error(
                        uniqueId = result.uniqueId,
                        errorCode = result.messageErrorCode.getName(),
                        errorDescription = result.messageErrorCode.getMessage()
                    )
                )
            }

            is ParsingResult.Success -> {
                receiveMessage(ocppSessionInfo, result.value)
            }

            is ParsingResult.Ignored -> {
                logger.warn("ignoring message type ${result.messageTypeId} (uniqueId=${result.uniqueId}): ${result.reason}")
            }
        }
    }

    suspend fun receiveMessage(
        ocppSessionInfo: OcppSession.Info,
        message: Message
    ) {
        when (message) {
            is Message.Request -> onRequest(ocppSessionInfo, message)
            is Message.Response -> onResponse(ocppSessionInfo, message)
            is Message.Error -> onError(ocppSessionInfo, message)
            is Message.Send -> onSend(ocppSessionInfo, message)
            is Message.CallResultError -> onCallResultError(ocppSessionInfo, message)
        }
    }

    /**
     * Sends an OCPP 2.1 SEND message (message type 6). SEND messages are unconfirmed: no response is awaited.
     */
    suspend fun sendUnconfirmed(
        ocppSessionInfo: OcppSession.Info,
        ocppRequest: OcppRequest
    ) {
        val feature = requestTypeToFeature[ocppRequest::class.java]

        requireNotNull(feature) { "feature not found for request ${ocppRequest::class.java}" }

        sendMessage(
            ocppSessionInfo = ocppSessionInfo,
            message = Message.Send(
                uniqueId = UUID.randomUUID().toString(),
                action = feature.name,
                payload = messageSerializer.toPayload(
                    value = ocppRequest
                )
            )
        )
    }

    suspend fun send(
        ocppSessionInfo: OcppSession.Info,
        ocppRequest: OcppRequest,
        timeout: Duration? = null
    ): OcppConfirmation {
        require(ocppRequest !is OcppUnconfirmedRequest) {
            "${ocppRequest.actionName()} is an unconfirmed (SEND) message without a response: use sendUnconfirmed()"
        }
        val feature = requestTypeToFeature[ocppRequest::class.java]

        requireNotNull(feature) { "feature not found for request ${ocppRequest::class.java}" }

        val request = Message.Request(
            uniqueId = UUID.randomUUID().toString(),
            action = feature.name,
            payload = messageSerializer.toPayload(
                value = ocppRequest
            )
        )

        try {
            val result = withTimeout(timeout ?: settings.defaultMessageTimeout) {
                val completable = completeDeferrableRepository.addRequest(
                    ocppSessionInfo.identity,
                    request.uniqueId
                ) { completableDeferred: CompletableDeferred<OcppConfirmation> ->
                    DeferredCache(request.uniqueId, request.action, completableDeferred)
                }

                // this should never happen, as we have just generated the uniqueId above as an UUID
                if (completable == null) {
                    throw OcppCallException(
                        errorCode = ocppErrorResponder.getInternalError(),
                        msg = "Duplicate message for the ${ocppSessionInfo.identity} - reusing ${request.uniqueId}"
                    )
                }

                sendMessage(
                    ocppSessionInfo = ocppSessionInfo,
                    message = request
                )

                completable.await()
            }
            return result
        } catch (timeoutException: TimeoutCancellationException) {
            // cleanup the timed out deferrable
            completeDeferrableRepository.getCachedDeferred(ocppSessionInfo.identity, request.uniqueId)
            throw OcppCallException(
                errorCode = ocppErrorResponder.getInternalError(),
                msg = "Timeout for '${feature.name}'",
                throwable = timeoutException
            )
        } finally {
            // restore the MDC context on the receiving thread
            MDC.put("labels.charge_point_identity", ocppSessionInfo.identity)
            MDC.put("labels.session_index", ocppSessionInfo.sessionIndex.uuid.toString())
        }
    }

    suspend fun sendError(
        ocppSessionInfo: OcppSession.Info,
        json: String,
        ocppErrorCode: OcppErrorCode,
        errorDescription: String = ocppErrorCode.getMessage()
    ) {
        val messageId: String? = messageSerializer.parseMessageId(json).getOrNull()

        if (messageId == null) {
            logger.warn("failed to parse id for message $json")
            return
        }

        sendMessage(
            ocppSessionInfo = ocppSessionInfo,
            message = Message.Error(
                uniqueId = messageId,
                errorCode = ocppErrorCode.getName(),
                errorDescription = errorDescription
            )
        )
    }

    fun <T : OcppRequest> parseOCPPRequest(
        message: Message.Request,
        requestType: Class<T>
    ): T {
        val request = messageSerializer.deserializePayload(message, requestType)
        return if (request is ParsingResult.Success) {
            request.value
        } else {
            throw IllegalArgumentException("The message can not be parsed as a $requestType")
        }
    }

    private suspend fun onRequest(
        ocppSessionInfo: OcppSession.Info,
        message: Message.Request
    ) {
        // If a null is returned we should just stop here, as the error is handled in the function
        val (profile, feature) = getProfileAndFeature(ocppSessionInfo, message.uniqueId, message.action) ?: return
        // If we get a null result for the getPayload function Just end here because the error has already been handled
        val request = getPayload(
            ocppSessionInfo = ocppSessionInfo,
            message = message,
            ignoreFailure = false,
            clazz = feature.requestType
        ) ?: return

        handleParsedRequest(profile, ocppSessionInfo, request, message)
    }

    protected open suspend fun handleParsedRequest(
        profile: ProfileDispatcher,
        ocppSessionInfo: OcppSession.Info,
        request: OcppRequest,
        message: Message.Request
    ) {
        val result = runCatching {
            profile.handleRequest(
                ocppSessionInfo = ocppSessionInfo,
                request = request
            )
        }

        if (result.isFailure) {
            val throwable = result.exceptionOrNull()

            when (throwable) {
                is OcppCallException -> {
                    if (throwable.errorCode.getName() == "NotSupported") {
                        logger.warn("Message with action [${message.action}] not supported (profile $profile)")
                    } else {
                        logger.error("Uncaught OCPP error (${throwable.errorCode.getName()}) for message with action [${message.action}] (profile $profile)", throwable)
                    }

                    sendMessage(
                        ocppSessionInfo = ocppSessionInfo,
                        message = throwable.toMessageError(message.uniqueId)
                    )
                }

                else -> {
                    logger.error("Uncaught exception for message with action [${message.action}] (profile $profile)", throwable)
                    sendMessage(
                        ocppSessionInfo = ocppSessionInfo,
                        message = OcppCallException.fromException(
                            ocppErrorCode = ocppErrorResponder.getInternalError(),
                            throwable = throwable
                        )
                            .toMessageError(message.uniqueId)
                    )
                }
            }
        } else {
            sendMessage(
                ocppSessionInfo = ocppSessionInfo,
                message = Message.Response(
                    uniqueId = message.uniqueId,
                    payload = messageSerializer.toPayload(result.getOrThrow())
                )
            )
        }
    }

    private suspend fun onResponse(
        ocppSessionInfo: OcppSession.Info,
        message: Message.Response
    ) {
        val deferredCache = completeDeferrableRepository.getCachedDeferred(
            identity = ocppSessionInfo.identity,
            id = message.uniqueId
        )

        if (deferredCache == null) {
            logger.warn("deferredCache not found for confirmation uniqueId=${message.uniqueId}")
            return
        }

        // Get the feature associated with the cache'd request
        val (_, feature) = getProfileAndFeature(ocppSessionInfo, deferredCache.uniqueId, deferredCache.action)
            ?: return
        if (ocpp21) {
            onResponse21(ocppSessionInfo, message, deferredCache, feature)
            return
        }
        // If we get a null result for the getPayload function just end here because the error has already been handled
        val confirmation = getPayload(
            ocppSessionInfo = ocppSessionInfo,
            message = message,
            ignoreFailure = true,
            clazz = feature.confirmationType
        ) ?: return
        // Lastly we send that confirmation up to our completable deferred
        deferredCache.complete(confirmation)
    }

    /**
     * OCPP 2.1: a CALLRESULT that cannot be processed is answered with a CALLRESULTERROR (OCPP-J 2.1, part 4, 4.2.3), and
     * the waiting [send] fails at once instead of timing out. 1.6 and 2.0.1 keep logging and dropping it.
     */
    private suspend fun onResponse21(
        ocppSessionInfo: OcppSession.Info,
        message: Message.Response,
        deferredCache: DeferredCache,
        feature: Feature
    ) {
        when (val result = messageSerializer.deserializePayload(message, feature.confirmationType)) {
            is ParsingResult.Success -> deferredCache.complete(result.value)
            is ParsingResult.Failure -> {
                logger.warn("Failed to parse ${feature.name} response $result")
                val errorCode = ocppErrorResponder.getJsonFormatError().getName()
                val description = "Invalid ${feature.name}Response: ${failureReason(result.throwable)}".take(MAX_ERROR_DESCRIPTION_LENGTH)
                sendMessage(
                    ocppSessionInfo = ocppSessionInfo,
                    message = Message.CallResultError(
                        uniqueId = message.uniqueId,
                        errorCode = errorCode,
                        errorDescription = description
                    )
                )
                deferredCache.completeExceptionally(ResponseException(errorCode, description, "{}"))
            }
            is ParsingResult.Ignored -> Unit
        }
    }

    /** The innermost cause's first line, e.g. "model length > maximum 20 - 21" (Jackson wraps the actual reason). */
    private fun failureReason(throwable: Throwable): String {
        return generateSequence(throwable) { it.cause }
            .mapNotNull { it.message?.lineSequence()?.firstOrNull()?.trim() }
            .lastOrNull { it.isNotBlank() }
            ?: throwable::class.simpleName.orEmpty()
    }

    private fun onError(
        ocppSessionInfo: OcppSession.Info,
        message: Message.Error
    ) {
        val requestWrapper = completeDeferrableRepository.getCachedDeferred(
            identity = ocppSessionInfo.identity,
            id = message.uniqueId
        )

        if (requestWrapper == null) {
            logger.debug("promise not found for error {}", message)
            return
        }

        requestWrapper.completeExceptionally(
            ResponseException(
                message.errorCode,
                message.errorDescription,
                message.errorDetails
            )
        )
    }

    /**
     * Handles an incoming OCPP 2.1 SEND message. The receiver SHALL NOT respond to a SEND, so failures are only logged.
     */
    private suspend fun onSend(
        ocppSessionInfo: OcppSession.Info,
        message: Message.Send
    ) {
        val (profile, feature) = actionToProfile[message.action] ?: run {
            logger.warn("ignoring SEND message with unknown action [${message.action}] (uniqueId=${message.uniqueId})")
            return
        }

        val request = when (val result = messageSerializer.deserializePayload(message, feature.requestType)) {
            is ParsingResult.Success -> result.value
            is ParsingResult.Failure -> {
                logger.warn("Failed to parse SEND message $result")
                return
            }
            is ParsingResult.Ignored -> return
        }

        runCatching {
            profile.handleRequest(
                ocppSessionInfo = ocppSessionInfo,
                request = request
            )
        }.onFailure { throwable ->
            logger.warn("Failed to handle SEND message with action [${message.action}] (profile $profile)", throwable)
        }
    }

    /**
     * Called when the other party could not process a CALLRESULT we sent (OCPP 2.1 CALLRESULTERROR).
     * There is nothing pending to complete, so by default this is only logged.
     */
    protected open suspend fun onCallResultError(
        ocppSessionInfo: OcppSession.Info,
        message: Message.CallResultError
    ) {
        logger.warn("received CALLRESULTERROR for our response uniqueId=${message.uniqueId}: ${message.errorCode} ${message.errorDescription}")
    }

    private suspend fun getProfileAndFeature(
        ocppSessionInfo: OcppSession.Info,
        uniqueId: String,
        actionName: String
    ): Pair<ProfileDispatcher, Feature>? {
        val result = actionToProfile[actionName]

        if (result == null) {
            sendMessage(
                ocppSessionInfo = ocppSessionInfo,
                message = Message.Error(
                    uniqueId = uniqueId,
                    errorCode = ocppErrorResponder.getNotImplementedError().getName(),
                    errorDescription = ocppErrorResponder.getNotImplementedError().getMessage()
                )
            )
            return null
        }

        return result
    }

    private suspend fun <T, R> getPayload(
        ocppSessionInfo: OcppSession.Info,
        message: R,
        ignoreFailure: Boolean,
        clazz: Class<T>
    ): T? where R : Message, R : Payloadable {
        return when (val result = messageSerializer.deserializePayload(message, clazz)) {
            is ParsingResult.Failure -> {
                if (!ignoreFailure) {
                    sendMessage(
                        ocppSessionInfo = ocppSessionInfo,
                        message = result.toError()
                    )
                }
                logger.warn("Failed to parse message $result")
                null
            }

            is ParsingResult.Success -> {
                result.value
            }

            is ParsingResult.Ignored -> null
        }
    }

    abstract suspend fun sendMessage(
        ocppSessionInfo: OcppSession.Info,
        message: Message
    )
}
