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

class RemoteStopTransactionSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse RemoteStopTransaction request" {
        val jsonString = TestUtils.getFileAsString("remotestoptransaction/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "RemoteStopTransaction"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"transactionId\":42}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, RemoteStopTransactionRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<RemoteStopTransactionRequest>>()
        payloadResult.value.transactionId shouldBe 42
    }

    "serialize RemoteStopTransaction request" {
        val request = RemoteStopTransactionRequest(transactionId = 42)
        val message = Message.Request("req-1", "RemoteStopTransaction", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"RemoteStopTransaction\",{\"transactionId\":42}]"
    }

    "parse RemoteStopTransaction response" {
        val jsonString = TestUtils.getFileAsString("remotestoptransaction/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Accepted\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, RemoteStopTransactionConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<RemoteStopTransactionConfirmation>>()
        payloadResult.value.status shouldBe RemoteStartStopStatus.Accepted
    }

    "serialize RemoteStopTransaction response" {
        val confirmation = RemoteStopTransactionConfirmation(status = RemoteStartStopStatus.Accepted)
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"status\":\"Accepted\"}]"
    }
})
