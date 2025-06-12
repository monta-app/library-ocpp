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

class ResetSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse Reset request" {
        val jsonString = TestUtils.getFileAsString("reset/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "Reset"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"type\":\"Hard\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, ResetRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<ResetRequest>>()
        payloadResult.value.type shouldBe ResetType.Hard
    }

    "serialize Reset request" {
        val request = ResetRequest(type = ResetType.Hard)
        val message = Message.Request("req-1", "Reset", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"Reset\",{\"type\":\"Hard\"}]"
    }

    "parse Reset response" {
        val jsonString = TestUtils.getFileAsString("reset/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Accepted\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, ResetConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<ResetConfirmation>>()
        payloadResult.value.status shouldBe ResetStatus.Accepted
    }

    "serialize Reset response" {
        val confirmation = ResetConfirmation(status = ResetStatus.Accepted)
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"status\":\"Accepted\"}]"
    }
})
