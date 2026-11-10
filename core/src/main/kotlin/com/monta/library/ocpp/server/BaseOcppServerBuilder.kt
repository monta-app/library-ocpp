package com.monta.library.ocpp.server

import com.monta.library.ocpp.common.OcppMessageListener
import com.monta.library.ocpp.common.OcppServerEventListener
import com.monta.library.ocpp.common.profile.ProfileDispatcher
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.session.OcppSession
import com.monta.library.ocpp.common.session.OcppSessionRepository
import com.monta.library.ocpp.common.transport.OcppSettings

@Suppress("UNCHECKED_CAST")
abstract class BaseOcppServerBuilder<T> {

    protected var onConnect: OcppServerEventListener? = null
    protected var onDisconnect: OcppServerEventListener? = null
    protected var ocppSessionRepository: OcppSessionRepository? = null
    protected var sendMessage: OcppMessageListener<Message>? = null
    protected var settings: OcppSettings? = null
    protected val profiles: MutableSet<ProfileDispatcher> = mutableSetOf()

    fun onConnect(
        onConnect: OcppServerEventListener
    ): T {
        this.onConnect = onConnect
        return this as T
    }

    fun onDisconnect(
        onDisconnect: OcppServerEventListener
    ): T {
        this.onDisconnect = onDisconnect
        return this as T
    }

    fun localMode(
        ocppSessionRepository: OcppSessionRepository
    ): T {
        this.ocppSessionRepository = ocppSessionRepository
        return this as T
    }

    /**
     * @param sendCallResultError listener for outgoing OCPP 2.1 CALLRESULTERROR messages (only needed for OCPP 2.1)
     * @param sendSend listener for outgoing OCPP 2.1 SEND messages (only needed for OCPP 2.1)
     */
    @JvmOverloads
    fun gatewayMode(
        sendRequest: OcppMessageListener<Message.Request>,
        sendResponse: OcppMessageListener<Message.Response>,
        sendError: OcppMessageListener<Message.Error>,
        sendCallResultError: OcppMessageListener<Message.CallResultError>? = null,
        sendSend: OcppMessageListener<Message.Send>? = null
    ): T {
        this.sendMessage =
            { messageSerializer: MessageSerializer, ocppSessionInfo: OcppSession.Info, message: Message ->
                when (message) {
                    is Message.Request -> {
                        sendRequest(messageSerializer, ocppSessionInfo, message)
                    }

                    is Message.Response -> {
                        sendResponse(messageSerializer, ocppSessionInfo, message)
                    }

                    is Message.Error -> {
                        sendError(messageSerializer, ocppSessionInfo, message)
                    }

                    is Message.CallResultError -> {
                        checkNotNull(sendCallResultError) { "gatewayMode: no listener configured for CALLRESULTERROR messages" }
                            .invoke(messageSerializer, ocppSessionInfo, message)
                    }

                    is Message.Send -> {
                        checkNotNull(sendSend) { "gatewayMode: no listener configured for SEND messages" }
                            .invoke(messageSerializer, ocppSessionInfo, message)
                    }
                }
            }
        return this as T
    }

    fun settings(
        settings: OcppSettings
    ): T {
        this.settings = settings
        return this as T
    }
}
