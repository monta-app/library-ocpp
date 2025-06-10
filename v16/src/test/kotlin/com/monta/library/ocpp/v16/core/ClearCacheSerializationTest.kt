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

class ClearCacheSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse ClearCache request" {
        val jsonString = TestUtils.getFileAsString("clearcache/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "ClearCache"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, ClearCacheRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<ClearCacheRequest>>()
    }

    "serialize ClearCache request" {
        val request = ClearCacheRequest
        val message = Message.Request("req-1", "ClearCache", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"ClearCache\",{}]"
    }

    "parse ClearCache response" {
        val jsonString = TestUtils.getFileAsString("clearcache/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Accepted\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, ClearCacheConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<ClearCacheConfirmation>>()
        payloadResult.value.status shouldBe ClearCacheStatus.Accepted
    }

    "serialize ClearCache response" {
        val confirmation = ClearCacheConfirmation(status = ClearCacheStatus.Accepted)
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"status\":\"Accepted\"}]"
    }
})
