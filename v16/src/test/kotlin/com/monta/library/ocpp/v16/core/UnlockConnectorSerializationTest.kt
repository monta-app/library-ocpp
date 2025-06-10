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

class UnlockConnectorSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse UnlockConnector request" {
        val jsonString = TestUtils.getFileAsString("unlockconnector/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "UnlockConnector"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"connectorId\":1}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, UnlockConnectorRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<UnlockConnectorRequest>>()
        payloadResult.value.connectorId shouldBe 1
    }

    "serialize UnlockConnector request" {
        val request = UnlockConnectorRequest(connectorId = 1)
        val message = Message.Request("req-1", "UnlockConnector", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"UnlockConnector\",{\"connectorId\":1}]"
    }

    "parse UnlockConnector response" {
        val jsonString = TestUtils.getFileAsString("unlockconnector/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Unlocked\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, UnlockConnectorConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<UnlockConnectorConfirmation>>()
        payloadResult.value.status shouldBe UnlockStatus.Unlocked
    }

    "serialize UnlockConnector response" {
        val confirmation = UnlockConnectorConfirmation(status = UnlockStatus.Unlocked)
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"status\":\"Unlocked\"}]"
    }
})
