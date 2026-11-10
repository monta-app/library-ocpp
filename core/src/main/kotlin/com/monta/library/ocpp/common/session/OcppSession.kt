package com.monta.library.ocpp.common.session

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.supervisorScope
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

open class OcppSession(
    val info: Info,
    private val sendFrame: suspend (message: String) -> Unit,
    private val closeConnection: suspend (closeReason: String) -> Unit,
    private val errorHandler: CoroutineExceptionHandler,
    private val sendHook: suspend (String, String) -> String? = { _, message -> message },
    settings: Settings = Settings()
) : Comparable<OcppSession> {

    private val sendChannel = Channel<String>(settings.defaultSendChannelSize)

    private val duplicate = AtomicBoolean()

    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.Unconfined) {
            // `supervisorScope` is used because we want to have continuous
            // processing of requestChannel even if one of the requests fails
            supervisorScope {
                launch(Dispatchers.IO + MDCContext() + errorHandler) {
                    sendChannel.consumeEach { message ->
                        sendFrame(message)
                    }
                }
            }
        }
    }

    suspend fun sendMessage(message: String) {
        val newMessage = sendHook(info.identity, message)
        if (newMessage != null) {
            sendChannel.send(newMessage)
        }
    }

    /**
     * Mark this session as a duplicate. This means that close calls will not trigger
     * calls to `closeFun`
     */
    fun markAsDuplicate() {
        duplicate.set(true)
    }

    suspend fun close(closeReason: String? = null) {
        try {
            // never call the `closeFun` twice, so if not a duplicate, we mark it as such now
            if (!duplicate.getAndSet(true)) {
                closeConnection(closeReason ?: "")
            }
        } finally {
            sendChannel.close()
        }
    }

    override fun compareTo(other: OcppSession): Int {
        return info.sessionIndex.compareTo(other.info.sessionIndex)
    }

    data class Info(
        val serverId: String,
        val identity: String,
        val ocppVersion: OcppVersion? = null,
        val sessionIndex: Index = Index()
    )

    enum class OcppVersion(
        val webSocketProtocol: String
    ) {
        V1_6("ocpp1.6"),
        V2_0_1("ocpp2.0.1"),
        V2_1("ocpp2.1");

        companion object {
            /**
             * The versions [valueOf] resolves. [V2_1] is deliberately left out so that existing consumers keep
             * selecting 1.6 / 2.0.1 exactly as before; OCPP 2.1 has to be opted into via [negotiate].
             */
            private val legacyProtocolsByString = listOf(V1_6, V2_0_1).associateBy { it.webSocketProtocol }

            private val protocolsByString = entries.associateBy { it.webSocketProtocol }

            /**
             * Returns the OCPP version based on the given websocket protocols.
             *
             * Note: Client preference (the order) is important, the first match is preferred.
             *
             * Only resolves [V1_6] and [V2_0_1] (see [negotiate] for OCPP 2.1).
             *
             * @return [default] if the websocket protocols are null or empty.
             * @throws IllegalArgumentException if the websocket protocols are not recognized.
             */
            @Suppress("unused")
            fun valueOf(
                webSocketProtocols: String?,
                default: OcppVersion
            ): OcppVersion {
                if (webSocketProtocols.isNullOrBlank()) return default

                return webSocketProtocols.split(',')
                    .firstNotNullOfOrNull {
                        legacyProtocolsByString[it.trim()]
                    } ?: throw IllegalArgumentException("Illegal websocket protocols '$webSocketProtocols'")
            }

            /**
             * Picks the OCPP version for a connection from the `Sec-WebSocket-Protocol` header value(s) offered by the
             * Charging Station, restricted to the versions in [supported].
             *
             * The Charging Station lists its subprotocols in order of preference (OCPP-J 2.1, part 4, section 3.1), so the
             * first offered subprotocol that is also in [supported] wins.
             *
             * @param webSocketProtocols comma separated subprotocols as sent by the Charging Station (may be null)
             * @param supported the versions the caller is able to speak
             * @return the negotiated version, or null when the header is absent/blank or none of the offered
             * subprotocols is supported
             */
            fun negotiate(
                webSocketProtocols: String?,
                supported: Set<OcppVersion>
            ): OcppVersion? {
                if (webSocketProtocols.isNullOrBlank()) return null

                return webSocketProtocols.split(',')
                    .firstNotNullOfOrNull { protocol ->
                        protocolsByString[protocol.trim()]?.takeIf { it in supported }
                    }
            }
        }
    }

    data class Index(
        val timestamp: Long = System.nanoTime(),
        val uuid: UUID = UUID.randomUUID()
    ) : Comparable<Index> {
        override fun compareTo(other: Index): Int {
            if (timestamp < other.timestamp) return -1
            if (timestamp > other.timestamp) return 1
            return uuid.compareTo(other.uuid)
        }
    }

    data class Settings(
        /**
         * The default size of the `send` channel
         */
        val defaultSendChannelSize: Int = 8
    )
}
