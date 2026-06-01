package com.monta.ocpp

import com.monta.library.ocpp.common.session.OcppSession
import com.monta.library.ocpp.common.session.OcppSessionRepository
import com.monta.library.ocpp.common.transport.OcppSettings
import com.monta.library.ocpp.v201.blocks.authorization.AuthorizationServerDispatcher
import com.monta.library.ocpp.v201.blocks.authorization.AuthorizeRequest
import com.monta.library.ocpp.v201.blocks.authorization.AuthorizeResponse
import com.monta.library.ocpp.v201.blocks.availability.AvailabilityServerDispatcher
import com.monta.library.ocpp.v201.blocks.availability.HeartbeatRequest
import com.monta.library.ocpp.v201.blocks.availability.HeartbeatResponse
import com.monta.library.ocpp.v201.blocks.availability.StatusNotificationRequest
import com.monta.library.ocpp.v201.blocks.availability.StatusNotificationResponse
import com.monta.library.ocpp.v201.blocks.provisioning.BootNotificationRequest
import com.monta.library.ocpp.v201.blocks.provisioning.BootNotificationResponse
import com.monta.library.ocpp.v201.blocks.provisioning.NotifyReportRequest
import com.monta.library.ocpp.v201.blocks.provisioning.NotifyReportResponse
import com.monta.library.ocpp.v201.blocks.provisioning.ProvisioningServerDispatcher
import com.monta.library.ocpp.v201.common.AuthorizationStatus
import com.monta.library.ocpp.v201.common.IdTokenInfo
import com.monta.library.ocpp.v201.server.OcppServerV201Builder
import io.ktor.serialization.jackson3.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Duration
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.zip.Deflater
import kotlin.time.toKotlinDuration

fun main(
    args: Array<String>
) = EngineMain.main(args)

private val logger = LoggerFactory.getLogger("WebSocket")

fun Application.module() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    // Serialization/Deserialization module
    install(ContentNegotiation) {
        jackson {
            MontaSerialization.withDefaults(this)
        }
    }
    // Call Logging
    install(CallLogging) {
        level = Level.INFO
    }
    // WebSockets
    install(WebSockets) {
        contentConverter = JacksonWebsocketContentConverter(
            MontaSerialization.getDefaultMapper()
        )

        pingPeriod = Duration.ofSeconds(15).toKotlinDuration()
        timeout = Duration.ofSeconds(15).toKotlinDuration()

        maxFrameSize = Long.MAX_VALUE
        masking = false

        extensions {
            install(WebSocketDeflateExtension) {
                /**
                 * Compression level to use for [java.util.zip.Deflater].
                 */
                compressionLevel = Deflater.DEFAULT_COMPRESSION

                /**
                 * Prevent compressing small outgoing frames.
                 */
                compressIfBiggerThan(bytes = 4 * 1024)
            }
        }
    }

    val ocppServer = OcppServerV201Builder()
        .onConnect { ocppSessionInfo ->
            logger.log(ocppSessionInfo, "onConnect")
        }
        .onDisconnect { ocppSessionInfo ->
            logger.log(ocppSessionInfo, "onDisconnect")
        }
        .localMode(OcppSessionRepository())
        .settings(
            OcppSettings(
                ocppExceptionHandler = { ocppSession, context, exception ->
                    logger.warn("[${ocppSession.identity}] coroutine exception", exception)
                }
            )
        )
        .addAuthorization(object : AuthorizationServerDispatcher.Listener {
            override suspend fun authorize(
                ocppSessionInfo: OcppSession.Info,
                request: AuthorizeRequest
            ): AuthorizeResponse {
                return AuthorizeResponse(
                    idTokenInfo = IdTokenInfo(
                        status = AuthorizationStatus.Accepted
                    )
                )
            }
        })
        .addProvisioning(object : ProvisioningServerDispatcher.Listener {
            override suspend fun bootNotification(
                ocppSessionInfo: OcppSession.Info,
                request: BootNotificationRequest
            ): BootNotificationResponse {
                logger.log(ocppSessionInfo, "boot notification $request")
                return BootNotificationResponse(
                    currentTime = ZonedDateTime.now(),
                    interval = 5,
                    status = BootNotificationResponse.Status.Accepted
                )
            }

            override suspend fun notifyReport(
                ocppSessionInfo: OcppSession.Info,
                request: NotifyReportRequest
            ): NotifyReportResponse {
                return NotifyReportResponse()
            }
        })
        .addAvailability(object : AvailabilityServerDispatcher.Listener {
            override suspend fun heartbeat(
                ocppSessionInfo: OcppSession.Info,
                request: HeartbeatRequest
            ): HeartbeatResponse {
                return HeartbeatResponse(
                    currentTime = ZonedDateTime.now()
                )
            }

            override suspend fun statusNotification(
                ocppSessionInfo: OcppSession.Info,
                request: StatusNotificationRequest
            ): StatusNotificationResponse {
                return StatusNotificationResponse()
            }
        })
        .build()

    // Ocpp
    routing {
        webSocket("/{identity}", protocol = "ocpp2.0.1") {
            val identity = call.parameters.getOrFail("identity")

            logger.log(identity, "charge point connected")

            ocppServer.connect(
                identity = identity,
                sendFrame = { message: String ->
                    logger.log(identity, "sending frame $message")
                    send(message)
                },
                closeConnection = { closeReason: String ->
                    close(
                        CloseReason(
                            CloseReason.Codes.NORMAL,
                            closeReason
                        )
                    )
                }
            )

            try {
                for (frame in this.incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val receivedText = frame.readText()
                            logger.log(identity, "received frame $receivedText")
                            ocppServer.receiveMessage(identity, receivedText)
                        }

                        else -> {
                            logger.warn("[$identity] unknown frame $frame")
                        }
                    }
                }
            } catch (e: Throwable) {
                logger.warn("[$identity] error ${closeReason.await()}", e)
            }

            ocppServer.disconnect(identity)
        }
    }
}

fun org.slf4j.Logger.log(ocppSessionInfo: OcppSession.Info, message: String) {
    log(ocppSessionInfo.identity, message)
}

fun org.slf4j.Logger.log(identity: String, message: String) {
    info("[$identity] $message")
}
