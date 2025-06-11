package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import com.monta.library.ocpp.v16.smartcharge.ChargingProfile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class RemoteStartTransactionSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse RemoteStartTransaction request with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("remote_start_transaction/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "RemoteStartTransaction"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"idTag\":\"TAG123\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, RemoteStartTransactionRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<RemoteStartTransactionRequest>>()
        payloadResult.value.idTag shouldBe "TAG123"
        payloadResult.value.connectorId shouldBe null
        payloadResult.value.chargingProfile shouldBe null
    }

    "serialize RemoteStartTransaction request with only mandatory fields" {
        val request = RemoteStartTransactionRequest(idTag = "TAG123")
        val message = Message.Request("req-1", "RemoteStartTransaction", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"RemoteStartTransaction\",{\"idTag\":\"TAG123\"}]"
    }

    "parse RemoteStartTransaction request with all optional fields" {
        val jsonString = TestUtils.getFileAsString("remote_start_transaction/req_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-2"
        ocppMessage.action shouldBe "RemoteStartTransaction"
        ocppMessage.payload.has("connectorId") shouldBe true
        ocppMessage.payload.has("chargingProfile") shouldBe true
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, RemoteStartTransactionRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<RemoteStartTransactionRequest>>()
        payloadResult.value.idTag shouldBe "TAG123"
        payloadResult.value.connectorId shouldBe 2
        payloadResult.value.chargingProfile.shouldBeInstanceOf<ChargingProfile>()
    }

    "serialize RemoteStartTransaction request with all optional fields" {
        val request = RemoteStartTransactionRequest(
            idTag = "TAG123",
            connectorId = 2,
            chargingProfile = ChargingProfile() // empty charging profile
        )
        val message = Message.Request("req-2", "RemoteStartTransaction", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer).contains("\"connectorId\":2") shouldBe true
        message.toJsonString(messageSerializer).contains("\"chargingProfile\":") shouldBe true
    }

    "parse RemoteStartTransaction response" {
        val jsonString = TestUtils.getFileAsString("remote_start_transaction/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Accepted\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, RemoteStartTransactionConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<RemoteStartTransactionConfirmation>>()
        payloadResult.value.status shouldBe RemoteStartStopStatus.Accepted
    }

    "serialize RemoteStartTransaction response" {
        val confirmation = RemoteStartTransactionConfirmation(status = RemoteStartStopStatus.Accepted)
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"status\":\"Accepted\"}]"
    }
})
