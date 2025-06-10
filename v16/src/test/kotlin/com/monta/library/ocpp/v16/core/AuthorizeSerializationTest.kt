package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.AuthorizationStatus
import com.monta.library.ocpp.v16.IdTagInfo
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AuthorizeSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse a valid authorize request" {
        val jsonString = TestUtils.getFileAsString("authorize/req.json")
        val parsingResult = messageSerializer.parse(jsonString)

        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "12345"
        ocppMessage.action shouldBe "Authorize"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"idTag\":\"ABC123\"}")

        val payloadResult = messageSerializer.deserializePayload(ocppMessage, AuthorizeRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<AuthorizeRequest>>()
        payloadResult.value.idTag shouldBe "ABC123"
    }

    "serialize an authorize request" {
        val request = AuthorizeRequest(idTag = "ABC123")
        val message = Message.Request("12345", "Authorize", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"12345\",\"Authorize\",{\"idTag\":\"ABC123\"}]"
    }

    "parse a valid authorize response" {
        val jsonString = TestUtils.getFileAsString("authorize/res.json")
        val parsingResult = messageSerializer.parse(jsonString)

        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "12345"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"idTagInfo\":{\"status\":\"Accepted\"}}")

        val payloadResult = messageSerializer.deserializePayload(ocppMessage, AuthorizeConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<AuthorizeConfirmation>>()
        payloadResult.value.idTagInfo.status shouldBe AuthorizationStatus.Accepted
    }

    "serialize an authorize response" {
        val confirmation = AuthorizeConfirmation(
            idTagInfo = IdTagInfo(status = AuthorizationStatus.Accepted)
        )
        val message = Message.Response("12345", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"12345\",{\"idTagInfo\":{\"status\":\"Accepted\"}}]"
    }

    "parse an authorize request with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("authorize/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "12345"
        ocppMessage.action shouldBe "Authorize"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"idTag\":\"ABC123\"}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, AuthorizeRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<AuthorizeRequest>>()
        payloadResult.value.idTag shouldBe "ABC123"
    }

    "parse an authorize response with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("authorize/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "12345"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"idTagInfo\":{\"status\":\"Accepted\"}}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, AuthorizeConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<AuthorizeConfirmation>>()
        payloadResult.value.idTagInfo.status shouldBe AuthorizationStatus.Accepted
    }

    "parse an authorize response with all optional fields" {
        val jsonString = TestUtils.getFileAsString("authorize/res_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "67890"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"idTagInfo\":{\"status\":\"Accepted\",\"expiryDate\":\"2025-12-31T23:59:59Z\",\"parentIdTag\":\"PARENT123\"}}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, AuthorizeConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<AuthorizeConfirmation>>()
        payloadResult.value.idTagInfo.status shouldBe AuthorizationStatus.Accepted
        payloadResult.value.idTagInfo.expiryDate.toString() shouldBe "2025-12-31T23:59:59Z"
        payloadResult.value.idTagInfo.parentIdTag shouldBe "PARENT123"
    }

    "serialize an authorize response with all optional fields" {
        val confirmation = AuthorizeConfirmation(
            idTagInfo = IdTagInfo(
                status = AuthorizationStatus.Accepted,
                expiryDate = java.time.ZonedDateTime.parse("2025-12-31T23:59:59Z"),
                parentIdTag = "PARENT123"
            )
        )
        val message = Message.Response("67890", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"67890\",{\"idTagInfo\":{\"status\":\"Accepted\",\"expiryDate\":\"2025-12-31T23:59:59Z\",\"parentIdTag\":\"PARENT123\"}}]"
    }
})
