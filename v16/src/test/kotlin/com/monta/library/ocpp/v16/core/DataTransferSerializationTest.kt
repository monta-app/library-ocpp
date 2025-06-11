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

class DataTransferSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse DataTransfer request with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("data_transfer/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "DataTransfer"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"vendorId\":\"VENDOR\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, DataTransferRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<DataTransferRequest>>()
        payloadResult.value.vendorId shouldBe "VENDOR"
        payloadResult.value.messageId shouldBe null
        payloadResult.value.data shouldBe null
    }

    "serialize DataTransfer request with only mandatory fields" {
        val request = DataTransferRequest(vendorId = "VENDOR")
        val message = Message.Request("req-1", "DataTransfer", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"DataTransfer\",{\"vendorId\":\"VENDOR\"}]"
    }

    "parse DataTransfer request with all optional fields" {
        val jsonString = TestUtils.getFileAsString("data_transfer/req_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-2"
        ocppMessage.action shouldBe "DataTransfer"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"vendorId\":\"VENDOR\",\"messageId\":\"MSGID\",\"data\":\"SOME_DATA\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, DataTransferRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<DataTransferRequest>>()
        payloadResult.value.vendorId shouldBe "VENDOR"
        payloadResult.value.messageId shouldBe "MSGID"
        payloadResult.value.data shouldBe "SOME_DATA"
    }

    "serialize DataTransfer request with all optional fields" {
        val request = DataTransferRequest(vendorId = "VENDOR", messageId = "MSGID", data = "SOME_DATA")
        val message = Message.Request("req-2", "DataTransfer", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-2\",\"DataTransfer\",{\"vendorId\":\"VENDOR\",\"messageId\":\"MSGID\",\"data\":\"SOME_DATA\"}]"
    }

    "parse DataTransfer response with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("data_transfer/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Accepted\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, DataTransferConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<DataTransferConfirmation>>()
        payloadResult.value.status shouldBe DataTransferStatus.Accepted
        payloadResult.value.data shouldBe null
    }

    "serialize DataTransfer response with only mandatory fields" {
        val confirmation = DataTransferConfirmation(status = DataTransferStatus.Accepted)
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{\"status\":\"Accepted\"}]"
    }

    "parse DataTransfer response with all optional fields" {
        val jsonString = TestUtils.getFileAsString("data_transfer/res_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-2"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Accepted\",\"data\":\"RESPONSE_DATA\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, DataTransferConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<DataTransferConfirmation>>()
        payloadResult.value.status shouldBe DataTransferStatus.Accepted
        payloadResult.value.data shouldBe "RESPONSE_DATA"
    }

    "serialize DataTransfer response with all optional fields" {
        val confirmation = DataTransferConfirmation(status = DataTransferStatus.Accepted, data = "RESPONSE_DATA")
        val message = Message.Response("res-2", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-2\",{\"status\":\"Accepted\",\"data\":\"RESPONSE_DATA\"}]"
    }
})
