package com.monta.library.ocpp.server

import com.monta.library.ocpp.common.OcppMessageListener
import com.monta.library.ocpp.common.OcppServerEventListener
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

open class OcppServer(
    private val onConnect: OcppServerEventListener,
    private val onDisconnect: OcppServerEventListener,
    private val sendMessage: OcppMessageListener<Message>?,
    private val ocppSessionRepository: OcppSessionRepository?,
    serializationMode: SerializationMode,
    ocppErrorResponder: OcppErrorResponder,
    settings: OcppSettings,
    profiles: Set<ProfileDispatcher>
) : OcppMessageInterpreter(serializationMode, ocppErrorResponder, logger, settings, profiles) {

    init {
        if (sendMessage == null && ocppSessionRepository == null) {
            throw IllegalStateException("server incorrectly configured")
        }
    }

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(OcppServer::class.java)
    }

    /**
     * Local functions
     */
    suspend fun connect(
        identity: String,
        sendFrame: suspend (String) -> Unit,
        closeConnection: suspend (closeReason: String) -> Unit
    ): OcppSession {
        if (ocppSessionRepository == null) {
            throw IllegalStateException("ocpp server not setup for local mode")
        }

        val ocppSession = OcppSession(
            info = OcppSession.Info(
                serverId = "",
                identity = identity
            ),
            sendFrame = sendFrame,
            closeConnection = closeConnection,
            errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                logger.warn("coroutine exception", throwable)
            }
        )

        ocppSessionRepository.add(identity, ocppSession)

        onConnect(ocppSession.info)

        return ocppSession
    }

    suspend fun disconnect(
        identity: String
    ) {
        val ocppSession = getSessionFromIdentity(identity)
        onDisconnect(ocppSession.info)
        ocppSessionRepository?.remove(identity)
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
        if (ocppSessionRepository == null) {
            throw IllegalStateException("ocpp server not setup for local mode")
        }

        val ocppSession = ocppSessionRepository.get(identity)

        if (ocppSession == null) {
            throw IllegalStateException("ocpp session not found for identity $identity")
        }

        return ocppSession
    }

    /**
     * Gateway functions
     */
    suspend fun connect(
        ocppSessionInfo: OcppSession.Info
    ) {
        onConnect(ocppSessionInfo)
    }

    suspend fun disconnect(
        ocppSessionInfo: OcppSession.Info
    ) {
        onDisconnect(ocppSessionInfo)
    }

    /**
     * Both?
     */
    override suspend fun sendMessage(
        ocppSessionInfo: OcppSession.Info,
        message: Message
    ) {
        if (ocppSessionRepository != null) {
            val ocppSession = getSessionFromIdentity(ocppSessionInfo.identity)
            ocppSession.sendMessage(message.toJsonString(messageSerializer))
        }

        if (sendMessage != null) {
            sendMessage.invoke(messageSerializer, ocppSessionInfo, message)
        }
    }
}
