package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.AuthorizationStatus
import com.monta.library.ocpp.v16.IdTagInfo
import com.monta.library.ocpp.v16.SampledValue
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime

class StopTransactionSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse a siemens stoptransaction with partly invalid transaction data (CPIBUG-61)" {
        val jsonString = TestUtils.getFileAsString("stop_transaction/stoptransaction-siemens.json")
        val parsingResult = messageSerializer.parse(jsonString)

        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "1700035413777"
        ocppMessage.action shouldBe "StopTransaction"

        val stopTransactionParsingResult = messageSerializer.deserializePayload(ocppMessage, StopTransactionRequest::class.java)
        stopTransactionParsingResult.shouldBeInstanceOf<ParsingResult.Success<StopTransactionRequest>>()
        val stopTransaction = stopTransactionParsingResult.value

        val transactionData = stopTransaction.transactionData.shouldNotBeNull()
        transactionData.size shouldBe 2
        transactionData.forEach {
            it.timestamp.shouldBeNull()
            it.sampledValue.shouldBeEmpty()
        }
    }

    "parse StopTransaction request with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("stop_transaction/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "StopTransaction"
        val expectedJson = "{" + "\"meterStop\":1000," + "\"timestamp\":\"2025-06-10T12:00:01Z\"," + "\"transactionId\":42}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, StopTransactionRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<StopTransactionRequest>>()
        payloadResult.value.meterStop shouldBe 1000
        payloadResult.value.timestamp.toString() shouldBe "2025-06-10T12:00:01Z"
        payloadResult.value.transactionId shouldBe 42
        payloadResult.value.idTag shouldBe null
        payloadResult.value.reason shouldBe null
        payloadResult.value.transactionData shouldBe null
    }

    "serialize StopTransaction request with only mandatory fields" {
        val request = StopTransactionRequest(
            meterStop = 1000,
            timestamp = ZonedDateTime.parse("2025-06-10T12:00:01Z"),
            transactionId = 42,
            transactionData = null
        )
        val message = Message.Request("req-1", "StopTransaction", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"StopTransaction\",{\"meterStop\":1000,\"timestamp\":\"2025-06-10T12:00:01Z\",\"transactionId\":42}]"
    }

    "parse StopTransaction request with all optional fields" {
        val jsonString = TestUtils.getFileAsString("stop_transaction/req_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-2"
        ocppMessage.action shouldBe "StopTransaction"
        val expectedJson =
            "{" + "\"meterStop\":1000," + "\"timestamp\":\"2025-06-10T12:00:01Z\"," + "\"transactionId\":42," + "\"idTag\":\"TAG123\"," + "\"reason\":\"DeAuthorized\"," + "\"transactionData\":[{" + "\"timestamp\":\"2025-06-10T12:00:01Z\"," + "\"sampledValue\":[{" + "\"value\":\"42\"" + "}]}]}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, StopTransactionRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<StopTransactionRequest>>()
        payloadResult.value.meterStop shouldBe 1000
        payloadResult.value.timestamp.toString() shouldBe "2025-06-10T12:00:01Z"
        payloadResult.value.transactionId shouldBe 42
        payloadResult.value.idTag shouldBe "TAG123"
        payloadResult.value.reason?.name shouldBe "DeAuthorized"
        payloadResult.value.transactionData?.size shouldBe 1
        payloadResult.value.transactionData?.get(0)?.timestamp.toString() shouldBe "2025-06-10T12:00:01Z"
        payloadResult.value.transactionData?.get(0)?.sampledValue?.get(0)?.value shouldBe "42"
    }

    "serialize StopTransaction request with all optional fields" {
        val request = StopTransactionRequest(
            meterStop = 1000,
            timestamp = ZonedDateTime.parse("2025-06-10T12:00:01Z"),
            transactionId = 42,
            idTag = "TAG123",
            reason = Reason.DeAuthorized,
            transactionData = listOf(
                MeterValue(
                    timestamp = ZonedDateTime.parse("2025-06-10T12:00:01Z"),
                    sampledValue = listOf(SampledValue(value = "42"))
                )
            )
        )
        val message = Message.Request("req-2", "StopTransaction", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-2\",\"StopTransaction\",{\"idTag\":\"TAG123\",\"meterStop\":1000,\"timestamp\":\"2025-06-10T12:00:01Z\",\"transactionId\":42,\"reason\":\"DeAuthorized\",\"transactionData\":[{\"timestamp\":\"2025-06-10T12:00:01Z\",\"sampledValue\":[{\"value\":\"42\",\"context\":\"Sample.Periodic\",\"format\":\"Raw\",\"measurand\":\"Energy.Active.Import.Register\",\"location\":\"Outlet\",\"unit\":\"Wh\"}]}]}]"
    }

    "parse StopTransaction response with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("stop_transaction/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, StopTransactionConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<StopTransactionConfirmation>>()
        payloadResult.value.idTagInfo shouldBe null
    }

    "serialize StopTransaction response with only mandatory fields" {
        val confirmation = StopTransactionConfirmation()
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{}]"
    }

    "parse StopTransaction response with all optional fields" {
        val jsonString = TestUtils.getFileAsString("stop_transaction/res_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-2"
        val expectedJson = "{\"idTagInfo\":{\"status\":\"Accepted\"}}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, StopTransactionConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<StopTransactionConfirmation>>()
        payloadResult.value.idTagInfo?.status?.name shouldBe "Accepted"
    }

    "serialize StopTransaction response with all optional fields" {
        val confirmation = StopTransactionConfirmation(
            idTagInfo = IdTagInfo(status = AuthorizationStatus.Accepted)
        )
        val message = Message.Response("res-2", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-2\",{\"idTagInfo\":{\"status\":\"Accepted\"}}]"
    }
})
