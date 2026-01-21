package com.monta.library.ocpp.client

import com.monta.library.ocpp.common.OcppClientConnectionEvent
import com.monta.library.ocpp.common.OcppClientDisconnectionEvent
import com.monta.library.ocpp.common.error.OcppErrorResponder
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import com.monta.library.ocpp.common.profile.ProfileDispatcher
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.common.session.OcppSession
import com.monta.library.ocpp.common.session.OcppSessionRepository
import com.monta.library.ocpp.common.transport.OcppMessageInterpreter
import com.monta.library.ocpp.common.transport.OcppSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import org.slf4j.LoggerFactory

open class OcppClient(
    private val onConnect: OcppClientConnectionEvent,
    private val onDisconnect: OcppClientDisconnectionEvent,
    private val ocppSessionRepository: OcppSessionRepository,
    private val sendHook: suspend (String, String) -> String?,
    serializationMode: SerializationMode,
    ocppErrorResponder: OcppErrorResponder,
    settings: OcppSettings,
    profiles: Set<ProfileDispatcher>
) : OcppMessageInterpreter(serializationMode, ocppErrorResponder, logger, settings, profiles) {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(OcppClient::class.java)
    }

    suspend fun connect(
        identity: String,
        isReconnecting: Boolean,
        sendFrame: suspend (String) -> Unit,
        closeConnection: suspend (closeReason: String) -> Unit
    ): OcppSession {
        val ocppSession = OcppSession(
            info = OcppSession.Info(
                serverId = "",
                identity = identity
            ),
            sendFrame = sendFrame,
            closeConnection = closeConnection,
            errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                logger.warn("coroutine exception", throwable)
            },
            sendHook = sendHook
        )

        ocppSessionRepository.add(identity, ocppSession)

        onConnect(ocppSession.info, isReconnecting)

        return ocppSession
    }

    suspend fun disconnect(
        identity: String,
        reason: String? = null
    ) {
        val ocppSession = getSessionFromIdentity(identity)
        ocppSessionRepository.remove(identity)
        ocppSession.close(reason)
    }

    suspend fun onDisconnect(
        identity: String
    ): Boolean {
        return onDisconnect(
            OcppSession.Info(
                serverId = "",
                identity = identity
            )
        )
    }

    suspend fun sendMessage(
        identity: String,
        ocppRequest: OcppRequest
    ): OcppConfirmation {
        val ocppSession = getSessionFromIdentity(identity)
        return send(ocppSession.info, ocppRequest)
    }

    suspend fun receiveMessage(
        identity: String,
        message: String
    ) {
        val ocppSession = getSessionFromIdentity(identity)
        receiveMessage(ocppSession.info, message)
    }

    private fun getSessionFromIdentity(
        identity: String
    ): OcppSession {
        val ocppSession = ocppSessionRepository.get(identity)

        if (ocppSession == null) {
            throw IllegalStateException("ocpp session not found for identity $identity")
        }

        return ocppSession
    }

    override suspend fun sendMessage(
        ocppSessionInfo: OcppSession.Info,
        message: Message
    ) {
        val ocppSession = getSessionFromIdentity(ocppSessionInfo.identity)
        ocppSession.sendMessage(message.toJsonString(messageSerializer))
    }

    fun getSessionByIdentity(
        identity: String
    ): OcppSession {
        val ocppSession = ocppSessionRepository.get(identity)
        if (ocppSession == null) throw RuntimeException("OcppSession not found for $identity")
        return ocppSession
    }
}
