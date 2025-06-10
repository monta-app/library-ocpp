package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class BootNotificationSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse BootNotification request with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("bootnotification/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "BootNotification"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(
            "{" +
                "\"chargePointVendor\":\"ACME\"," +
                "\"chargePointModel\":\"ModelX\"}"
        )
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, BootNotificationRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<BootNotificationRequest>>()
        payloadResult.value.chargePointVendor shouldBe "ACME"
        payloadResult.value.chargePointModel shouldBe "ModelX"
    }

    "serialize BootNotification request with only mandatory fields" {
        val request = BootNotificationRequest(
            chargePointVendor = "ACME",
            chargePointModel = "ModelX"
        )
        val message = Message.Request("req-1", "BootNotification", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"BootNotification\",{\"chargePointModel\":\"ModelX\",\"chargePointVendor\":\"ACME\"}]"
    }

    "parse BootNotification request with all optional fields" {
        val jsonString = TestUtils.getFileAsString("bootnotification/req_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-2"
        ocppMessage.action shouldBe "BootNotification"
        val expectedJson = "{" +
            "\"chargePointVendor\":\"ACME\"," +
            "\"chargePointModel\":\"ModelX\"," +
            "\"chargePointSerialNumber\":\"SN123\"," +
            "\"chargeBoxSerialNumber\":\"CB456\"," +
            "\"firmwareVersion\":\"1.2.3\"," +
            "\"iccid\":\"ICCID123\"," +
            "\"imsi\":\"IMSI456\"," +
            "\"meterType\":\"MeterTypeA\"," +
            "\"meterSerialNumber\":\"MTR789\"}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, BootNotificationRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<BootNotificationRequest>>()
        payloadResult.value.chargePointVendor shouldBe "ACME"
        payloadResult.value.chargePointModel shouldBe "ModelX"
        payloadResult.value.chargePointSerialNumber shouldBe "SN123"
        @Suppress("deprecation")
        payloadResult.value.chargeBoxSerialNumber shouldBe "CB456"
        payloadResult.value.firmwareVersion shouldBe "1.2.3"
        payloadResult.value.iccid shouldBe "ICCID123"
        payloadResult.value.imsi shouldBe "IMSI456"
        payloadResult.value.meterType shouldBe "MeterTypeA"
        payloadResult.value.meterSerialNumber shouldBe "MTR789"
    }

    "serialize BootNotification request with all optional fields" {
        val request = BootNotificationRequest(
            chargePointVendor = "ACME",
            chargePointModel = "ModelX",
            chargePointSerialNumber = "SN123",
            chargeBoxSerialNumber = "CB456",
            firmwareVersion = "1.2.3",
            iccid = "ICCID123",
            imsi = "IMSI456",
            meterType = "MeterTypeA",
            meterSerialNumber = "MTR789"
        )
        val message = Message.Request("req-2", "BootNotification", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-2\",\"BootNotification\",{\"chargeBoxSerialNumber\":\"CB456\",\"chargePointModel\":\"ModelX\",\"chargePointSerialNumber\":\"SN123\",\"chargePointVendor\":\"ACME\",\"firmwareVersion\":\"1.2.3\",\"iccid\":\"ICCID123\",\"imsi\":\"IMSI456\",\"meterSerialNumber\":\"MTR789\",\"meterType\":\"MeterTypeA\"}]"
    }

    "parse BootNotification response" {
        val jsonString = TestUtils.getFileAsString("bootnotification/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val expectedJson = "{" +
            "\"status\":\"Accepted\"," +
            "\"currentTime\":\"2025-06-10T12:00:01Z\"," +
            "\"interval\":300}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, BootNotificationConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<BootNotificationConfirmation>>()
        payloadResult.value.status shouldBe RegistrationStatus.Accepted
        payloadResult.value.currentTime.format(formatter) shouldBe "2025-06-10T12:00:01Z"
        payloadResult.value.interval shouldBe 300
    }

    "serialize BootNotification response" {
        val confirmation = BootNotificationConfirmation(
            status = RegistrationStatus.Accepted,
            currentTime = ZonedDateTime.parse("2025-06-10T12:00:01Z"),
            interval = 300
        )
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"currentTime\":\"2025-06-10T12:00:01Z\",\"interval\":300,\"status\":\"Accepted\"}]"
    }
})
