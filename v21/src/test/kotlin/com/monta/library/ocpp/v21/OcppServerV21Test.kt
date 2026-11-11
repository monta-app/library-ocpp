package com.monta.library.ocpp.v21

import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.common.session.OcppSession
import com.monta.library.ocpp.common.transport.OcppSettings
import com.monta.library.ocpp.common.transport.ResponseException
import com.monta.library.ocpp.v21.blocks.authorization.authorizationFeatures
import com.monta.library.ocpp.v21.blocks.availability.AvailabilityServerDispatcher
import com.monta.library.ocpp.v21.blocks.availability.HeartbeatRequest
import com.monta.library.ocpp.v21.blocks.availability.HeartbeatResponse
import com.monta.library.ocpp.v21.blocks.availability.StatusNotificationRequest
import com.monta.library.ocpp.v21.blocks.availability.StatusNotificationResponse
import com.monta.library.ocpp.v21.blocks.availability.availabilityFeatures
import com.monta.library.ocpp.v21.blocks.batteryswapping.batterySwappingFeatures
import com.monta.library.ocpp.v21.blocks.bidirectional.bidirectionalFeatures
import com.monta.library.ocpp.v21.blocks.certificatemanagement.certificateManagementFeatures
import com.monta.library.ocpp.v21.blocks.datatransfer.dataTransferFeatures
import com.monta.library.ocpp.v21.blocks.dercontrol.derControlFeatures
import com.monta.library.ocpp.v21.blocks.diagnostics.ClosePeriodicEventStreamRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.ClosePeriodicEventStreamResponse
import com.monta.library.ocpp.v21.blocks.diagnostics.DiagnosticsServerDispatcher
import com.monta.library.ocpp.v21.blocks.diagnostics.LogStatusNotificationRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.LogStatusNotificationResponse
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyCustomerInformationRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyCustomerInformationResponse
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyEventRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyEventResponse
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyMonitoringReportRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyMonitoringReportResponse
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyPeriodicEventStreamRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.OpenPeriodicEventStreamRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.OpenPeriodicEventStreamResponse
import com.monta.library.ocpp.v21.blocks.diagnostics.StreamDataElement
import com.monta.library.ocpp.v21.blocks.diagnostics.diagnosticsFeatures
import com.monta.library.ocpp.v21.blocks.displaymessage.displayMessageFeatures
import com.monta.library.ocpp.v21.blocks.firmwaremanagement.firmwareManagementFeatures
import com.monta.library.ocpp.v21.blocks.localauthorizationlistmanagement.localAuthorizationListManagementFeatures
import com.monta.library.ocpp.v21.blocks.metervalues.meterValuesFeatures
import com.monta.library.ocpp.v21.blocks.provisioning.ResetEnum
import com.monta.library.ocpp.v21.blocks.provisioning.ResetRequest
import com.monta.library.ocpp.v21.blocks.provisioning.provisioningFeatures
import com.monta.library.ocpp.v21.blocks.remotecontrol.remoteControlFeatures
import com.monta.library.ocpp.v21.blocks.reservation.reservationFeatures
import com.monta.library.ocpp.v21.blocks.security.securityFeatures
import com.monta.library.ocpp.v21.blocks.smartcharging.smartChargingFeatures
import com.monta.library.ocpp.v21.blocks.tariffandcost.tariffAndCostFeatures
import com.monta.library.ocpp.v21.blocks.transactions.transactionsFeatures
import com.monta.library.ocpp.v21.common.GenericStatusEnum
import com.monta.library.ocpp.v21.error.OcppErrorResponderV21
import com.monta.library.ocpp.v21.server.OcppServerV21Builder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime

class OcppServerV21Test : StringSpec({

    val info = OcppSession.Info(serverId = "test", identity = "CS-1", ocppVersion = OcppSession.OcppVersion.V2_1)

    fun server(sent: MutableList<Message>) = OcppServerV21Builder()
        .gatewayMode(
            sendRequest = { _, _, message -> sent.add(message) },
            sendResponse = { _, _, message -> sent.add(message) },
            sendError = { _, _, message -> sent.add(message) },
            sendCallResultError = { _, _, message -> sent.add(message) }
        )
        .onConnect { }
        .onDisconnect { }
        .settings(OcppSettings())
        .addAvailability(
            object : AvailabilityServerDispatcher.Listener {
                override suspend fun heartbeat(ocppSessionInfo: OcppSession.Info, request: HeartbeatRequest) = HeartbeatResponse(currentTime = ZonedDateTime.of(2025, 1, 23, 12, 0, 0, 0, ZoneOffset.UTC))

                override suspend fun statusNotification(ocppSessionInfo: OcppSession.Info, request: StatusNotificationRequest) = StatusNotificationResponse()
            }
        )
        .build()

    "every action belongs to exactly one block" {
        val blockFeatures = listOf(
            securityFeatures,
            provisioningFeatures,
            authorizationFeatures,
            localAuthorizationListManagementFeatures,
            transactionsFeatures,
            remoteControlFeatures,
            availabilityFeatures,
            reservationFeatures,
            tariffAndCostFeatures,
            meterValuesFeatures,
            smartChargingFeatures,
            firmwareManagementFeatures,
            certificateManagementFeatures,
            diagnosticsFeatures,
            displayMessageFeatures,
            dataTransferFeatures,
            bidirectionalFeatures,
            derControlFeatures,
            batterySwappingFeatures
        ).flatten()
        blockFeatures shouldHaveSize 91
        blockFeatures shouldContainExactlyInAnyOrder V21Actions.features
    }

    "a CALL to an added block is answered by its listener" {
        val sent = mutableListOf<Message>()
        server(sent).receiveMessage(info, """[2,"hb-1","Heartbeat",{}]""")
        sent shouldHaveSize 1
        val response = sent.single()
        response.shouldBeInstanceOf<Message.Response>()
        response.uniqueId shouldBe "hb-1"
        response.payload["currentTime"].asString() shouldBe "2025-01-23T12:00:00.000Z"
    }

    "a CALL to a block that was not added is answered with NotSupported" {
        val sent = mutableListOf<Message>()
        server(sent).receiveMessage(info, """[2,"ev-1","NotifyEvent",{"generatedAt":"2025-01-23T12:00:00.000Z","seqNo":0,"eventData":[]}]""")
        val error = sent.single()
        error.shouldBeInstanceOf<Message.Error>()
        error.uniqueId shouldBe "ev-1"
    }

    "SEND and unknown message types are never answered" {
        val sent = mutableListOf<Message>()
        val server = server(sent)
        server.receiveMessage(
            info,
            """[6,"s-1","NotifyPeriodicEventStream",{"id":1,"pending":0,"basetime":"2025-01-23T12:00:00.000Z","data":[{"t":1.5,"v":"230.4"}]}]"""
        )
        server.receiveMessage(info, """[7,"x-1","Future",{}]""")
        server.receiveMessage(info, """[5,"hb-1","FormatViolation","could not process",{}]""")
        sent.shouldBeEmpty()
    }

    "blocks that were not added can still be sent to the station" {
        val sent = mutableListOf<Message>()
        val server = server(sent)
        // Provisioning was not added; sending Reset must still find its feature (the call then times out without a station).
        provisioningFeatures.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "BootNotification",
            "GetBaseReport",
            "GetReport",
            "GetVariables",
            "NotifyReport",
            "Reset",
            "SetNetworkProfile",
            "SetVariables"
        )
        val sender = server.asProvisioningBlock(info)
        runCatching {
            withTimeout(200) {
                sender.reset(ResetRequest(type = ResetEnum.Immediate))
            }
        }
        val request = sent.single()
        request.shouldBeInstanceOf<Message.Request>()
        request.action shouldBe "Reset"
        request.toJsonString(MessageSerializer(SerializationMode.OCPP_2_1, OcppErrorResponderV21)) shouldStartWith "[2,"
    }
    "an invalid CALLRESULT is answered with CALLRESULTERROR and fails the waiting call at once" {
        val sent = mutableListOf<Message>()
        val server = server(sent)
        coroutineScope {
            val call = async {
                runCatching { server.asProvisioningBlock(info).reset(ResetRequest(type = ResetEnum.Immediate)) }
            }
            while (sent.isEmpty()) delay(10)
            val request = sent.single() as Message.Request
            server.receiveMessage(info, """[3,"${request.uniqueId}",{"status":"NotAStatus"}]""")
            val failure = call.await().exceptionOrNull()
            failure.shouldBeInstanceOf<ResponseException>()
            failure.errorCode shouldBe "FormatViolation"
            val error = sent.last()
            error.shouldBeInstanceOf<Message.CallResultError>()
            error.uniqueId shouldBe request.uniqueId
            error.errorDescription shouldStartWith "Invalid ResetResponse"
        }
    }

    "SEND-only requests cannot be sent as a CALL" {
        val server = server(mutableListOf())
        val request = NotifyPeriodicEventStreamRequest(
            id = 1,
            pending = 0,
            basetime = ZonedDateTime.of(2025, 1, 23, 12, 0, 0, 0, ZoneOffset.UTC),
            data = listOf(StreamDataElement(t = BigDecimal("1.5"), v = "230.4"))
        )
        shouldThrow<IllegalArgumentException> { server.send(info, request) }
    }

    "NotifyPeriodicEventStream sent as a CALL gets an empty CALLRESULT (not defined by the spec)" {
        val sent = mutableListOf<Message>()
        val received = mutableListOf<NotifyPeriodicEventStreamRequest>()
        val server = OcppServerV21Builder()
            .gatewayMode(
                sendRequest = { _, _, message -> sent.add(message) },
                sendResponse = { _, _, message -> sent.add(message) },
                sendError = { _, _, message -> sent.add(message) }
            )
            .onConnect { }
            .onDisconnect { }
            .settings(OcppSettings())
            .addDiagnostics(
                object : DiagnosticsServerDispatcher.Listener {
                    override suspend fun closePeriodicEventStream(ocppSessionInfo: OcppSession.Info, request: ClosePeriodicEventStreamRequest) = ClosePeriodicEventStreamResponse()

                    override suspend fun logStatusNotification(ocppSessionInfo: OcppSession.Info, request: LogStatusNotificationRequest) = LogStatusNotificationResponse()

                    override suspend fun notifyCustomerInformation(ocppSessionInfo: OcppSession.Info, request: NotifyCustomerInformationRequest) = NotifyCustomerInformationResponse()

                    override suspend fun notifyEvent(ocppSessionInfo: OcppSession.Info, request: NotifyEventRequest) = NotifyEventResponse()

                    override suspend fun notifyMonitoringReport(ocppSessionInfo: OcppSession.Info, request: NotifyMonitoringReportRequest) = NotifyMonitoringReportResponse()

                    override suspend fun notifyPeriodicEventStream(ocppSessionInfo: OcppSession.Info, request: NotifyPeriodicEventStreamRequest) {
                        received.add(request)
                    }

                    override suspend fun openPeriodicEventStream(ocppSessionInfo: OcppSession.Info, request: OpenPeriodicEventStreamRequest) = OpenPeriodicEventStreamResponse(status = GenericStatusEnum.Accepted)
                }
            )
            .build()
        val payload = """{"id":1,"pending":0,"basetime":"2025-01-23T12:00:00.000Z","data":[{"t":1.5,"v":"230.4"}]}"""
        // As a SEND (the defined way): handled, never answered
        server.receiveMessage(info, """[6,"s-1","NotifyPeriodicEventStream",$payload]""")
        received shouldHaveSize 1
        sent.shouldBeEmpty()
        // As a CALL: handled too, and answered with an empty CALLRESULT
        server.receiveMessage(info, """[2,"c-1","NotifyPeriodicEventStream",$payload]""")
        received shouldHaveSize 2
        val response = sent.single()
        response.shouldBeInstanceOf<Message.Response>()
        response.payload.isEmpty shouldBe true
    }
})
