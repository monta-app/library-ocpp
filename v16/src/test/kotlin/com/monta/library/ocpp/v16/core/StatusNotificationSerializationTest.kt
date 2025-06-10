package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime

class StatusNotificationSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse StatusNotification request with only mandatory fields" {
        val testStartTime = ZonedDateTime.now()
        val jsonString = TestUtils.getFileAsString("statusnotification/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "StatusNotification"
        val expectedJson = "{" +
            "\"connectorId\":1," +
            "\"errorCode\":\"NoError\"," +
            "\"status\":\"Available\"}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, StatusNotificationRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<StatusNotificationRequest>>()
        payloadResult.value.connectorId shouldBe 1
        payloadResult.value.errorCode shouldBe ChargePointErrorCode.NoError
        payloadResult.value.status shouldBe ChargePointStatus.Available
        payloadResult.value.timestamp shouldBeAfter testStartTime // Optional. The time for which the status is reported. If absent time  of receipt of the message will be assumed.
        payloadResult.value.info shouldBe null
        payloadResult.value.vendorId shouldBe null
        payloadResult.value.vendorErrorCode shouldBe null
    }

    "serialize StatusNotification request with only mandatory fields" {
        val request = StatusNotificationRequest(
            connectorId = 1,
            errorCode = ChargePointErrorCode.NoError,
            status = ChargePointStatus.Available,
            timestamp = ZonedDateTime.parse("2025-06-10T12:00:01Z")
        )
        val message = Message.Request("req-1", "StatusNotification", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"StatusNotification\",{\"connectorId\":1,\"errorCode\":\"NoError\",\"status\":\"Available\",\"timestamp\":\"2025-06-10T12:00:01Z\"}]"
    }

    "parse StatusNotification request with all optional fields" {
        val jsonString = TestUtils.getFileAsString("statusnotification/req_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-2"
        ocppMessage.action shouldBe "StatusNotification"
        val expectedJson = "{" +
            "\"connectorId\":1," +
            "\"errorCode\":\"NoError\"," +
            "\"status\":\"Available\"," +
            "\"timestamp\":\"2025-06-10T12:00:01Z\"," +
            "\"info\":\"Test info\"," +
            "\"vendorId\":\"VENDOR\"," +
            "\"vendorErrorCode\":\"ERR123\"}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, StatusNotificationRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<StatusNotificationRequest>>()
        payloadResult.value.connectorId shouldBe 1
        payloadResult.value.errorCode shouldBe ChargePointErrorCode.NoError
        payloadResult.value.status shouldBe ChargePointStatus.Available
        payloadResult.value.timestamp.toString() shouldBe "2025-06-10T12:00:01Z"
        payloadResult.value.info shouldBe "Test info"
        payloadResult.value.vendorId shouldBe "VENDOR"
        payloadResult.value.vendorErrorCode shouldBe "ERR123"
    }

    "serialize StatusNotification request with all optional fields" {
        val request = StatusNotificationRequest(
            connectorId = 1,
            errorCode = ChargePointErrorCode.NoError,
            status = ChargePointStatus.Available,
            timestamp = ZonedDateTime.parse("2025-06-10T12:00:01Z"),
            info = "Test info",
            vendorId = "VENDOR",
            vendorErrorCode = "ERR123"
        )
        val message = Message.Request("req-2", "StatusNotification", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-2\",\"StatusNotification\",{\"connectorId\":1,\"errorCode\":\"NoError\",\"info\":\"Test info\",\"status\":\"Available\",\"timestamp\":\"2025-06-10T12:00:01Z\",\"vendorId\":\"VENDOR\",\"vendorErrorCode\":\"ERR123\"}]"
    }

    "parse StatusNotification response" {
        val jsonString = TestUtils.getFileAsString("statusnotification/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, StatusNotificationConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<StatusNotificationConfirmation>>()
    }

    "serialize StatusNotification response" {
        val confirmation = StatusNotificationConfirmation
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{}]"
    }
})
