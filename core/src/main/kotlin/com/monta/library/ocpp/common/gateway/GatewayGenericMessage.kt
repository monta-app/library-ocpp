package com.monta.library.ocpp.common.gateway

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.session.OcppSession
import java.time.Instant

/**
 * A message being sent *from* a gateway node to *any* processor node.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
sealed class GatewayGenericMessage(
    open val ocppSessionInfo: OcppSession.Info,
    open val messageAt: Instant = Instant.now()
) {
    data class Connect(
        override val ocppSessionInfo: OcppSession.Info
    ) : GatewayGenericMessage(ocppSessionInfo)

    data class OcppRequest(
        override val ocppSessionInfo: OcppSession.Info,
        val request: Message.Request,
        val handledOnGateway: Boolean
    ) : GatewayGenericMessage(ocppSessionInfo)

    data class Disconnect(
        override val ocppSessionInfo: OcppSession.Info
    ) : GatewayGenericMessage(ocppSessionInfo)

    data class SyncConnectivity(
        override val ocppSessionInfo: OcppSession.Info
    ) : GatewayGenericMessage(ocppSessionInfo)
}
