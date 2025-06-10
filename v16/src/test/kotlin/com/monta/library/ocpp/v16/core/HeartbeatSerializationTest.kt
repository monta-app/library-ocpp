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

class HeartbeatSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse Heartbeat request" {
        val jsonString = TestUtils.getFileAsString("heartbeat/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "Heartbeat"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, HeartbeatRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<HeartbeatRequest>>()
    }

    "serialize Heartbeat request" {
        val request = HeartbeatRequest
        val message = Message.Request("req-1", "Heartbeat", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"Heartbeat\",{}]"
    }

    "parse Heartbeat response" {
        val jsonString = TestUtils.getFileAsString("heartbeat/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val expectedJson = "{\"currentTime\":\"2025-06-10T12:00:01Z\"}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, HeartbeatConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<HeartbeatConfirmation>>()
        payloadResult.value.currentTime.format(formatter) shouldBe "2025-06-10T12:00:01Z"
    }

    "serialize Heartbeat response" {
        val confirmation = HeartbeatConfirmation(
            currentTime = ZonedDateTime.parse("2025-06-10T12:00:01Z")
        )
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"currentTime\":\"2025-06-10T12:00:01Z\"}]"
    }
})
