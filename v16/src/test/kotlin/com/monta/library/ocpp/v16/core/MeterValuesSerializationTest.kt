package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.Context
import com.monta.library.ocpp.v16.Location
import com.monta.library.ocpp.v16.Measurand
import com.monta.library.ocpp.v16.Phase
import com.monta.library.ocpp.v16.SampledValue
import com.monta.library.ocpp.v16.Unit
import com.monta.library.ocpp.v16.ValueFormat
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime

class MeterValuesSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse MeterValues request with only mandatory fields" {
        val jsonString = TestUtils.getFileAsString("metervalues/req.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-1"
        ocppMessage.action shouldBe "MeterValues"
        val expectedJson = "{" +
            "\"connectorId\":1," +
            "\"meterValue\":[{" +
            "\"timestamp\":\"2025-06-10T12:00:01Z\"," +
            "\"sampledValue\":[{" +
            "\"value\":\"42\"" +
            "}]}]}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, MeterValuesRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<MeterValuesRequest>>()
        payloadResult.value.connectorId shouldBe 1
        payloadResult.value.transactionId shouldBe null
        payloadResult.value.meterValue.size shouldBe 1
        payloadResult.value.meterValue[0].timestamp.toString() shouldBe "2025-06-10T12:00:01Z"
        payloadResult.value.meterValue[0].sampledValue.size shouldBe 1
        payloadResult.value.meterValue[0].sampledValue[0].value shouldBe "42"
    }

    "serialize MeterValues request with only mandatory fields" {
        val request = MeterValuesRequest(
            connectorId = 1,
            meterValue = listOf(
                MeterValue(
                    timestamp = ZonedDateTime.parse("2025-06-10T12:00:01Z"),
                    sampledValue = listOf(SampledValue(value = "42"))
                )
            )
        )
        val message = Message.Request("req-1", "MeterValues", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-1\",\"MeterValues\",{\"connectorId\":1,\"meterValue\":[{\"timestamp\":\"2025-06-10T12:00:01Z\",\"sampledValue\":[{\"value\":\"42\",\"context\":\"Sample.Periodic\",\"format\":\"Raw\",\"measurand\":\"Energy.Active.Import.Register\",\"location\":\"Outlet\",\"unit\":\"Wh\"}]}]}]"
    }

    "parse MeterValues request with all fields" {
        val jsonString = TestUtils.getFileAsString("metervalues/req_optional.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "req-2"
        ocppMessage.action shouldBe "MeterValues"
        val expectedJson = "{" +
            "\"connectorId\":1," +
            "\"transactionId\":123," +
            "\"meterValue\":[{" +
            "\"timestamp\":\"2025-06-10T12:00:01Z\"," +
            "\"sampledValue\":[{" +
            "\"value\":\"42\"," +
            "\"context\":\"Sample.Clock\"," +
            "\"format\":\"Raw\"," +
            "\"measurand\":\"Energy.Active.Import.Register\"," +
            "\"phase\":\"L1\"," +
            "\"location\":\"EV\"," +
            "\"unit\":\"Wh\"" +
            "}]}]}"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(expectedJson)
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, MeterValuesRequest::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<MeterValuesRequest>>()
        payloadResult.value.connectorId shouldBe 1
        payloadResult.value.transactionId shouldBe 123
        payloadResult.value.meterValue.size shouldBe 1
        payloadResult.value.meterValue[0].timestamp.toString() shouldBe "2025-06-10T12:00:01Z"
        val sv = payloadResult.value.meterValue[0].sampledValue[0]
        sv.value shouldBe "42"
        sv.context shouldBe "Sample.Clock"
        sv.format shouldBe "Raw"
        sv.measurand shouldBe "Energy.Active.Import.Register"
        sv.phase shouldBe "L1"
        sv.location shouldBe "EV"
        sv.unit shouldBe "Wh"
    }

    "serialize MeterValues request with all fields" {
        val request = MeterValuesRequest(
            connectorId = 1,
            transactionId = 123,
            meterValue = listOf(
                MeterValue(
                    timestamp = ZonedDateTime.parse("2025-06-10T12:00:01Z"),
                    sampledValue = listOf(
                        SampledValue(
                            value = "42",
                            context = Context.SampleClock.value,
                            format = ValueFormat.Raw.name,
                            measurand = Measurand.EnergyActiveImportRegister.value,
                            phase = Phase.L1.value,
                            location = Location.EV.name,
                            unit = Unit.WattHour.value
                        )
                    )
                )
            )
        )
        val message = Message.Request("req-2", "MeterValues", messageSerializer.toPayload(request))
        message.toJsonString(messageSerializer) shouldBe "[2,\"req-2\",\"MeterValues\",{\"connectorId\":1,\"transactionId\":123,\"meterValue\":[{\"timestamp\":\"2025-06-10T12:00:01Z\",\"sampledValue\":[{\"value\":\"42\",\"context\":\"Sample.Clock\",\"format\":\"Raw\",\"measurand\":\"Energy.Active.Import.Register\",\"phase\":\"L1\",\"location\":\"EV\",\"unit\":\"Wh\"}]}]}]"
    }

    "parse MeterValues response" {
        val jsonString = TestUtils.getFileAsString("metervalues/res.json")
        val parsingResult = messageSerializer.parse(jsonString)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value
        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "res-1"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{}")
        val payloadResult = messageSerializer.deserializePayload(ocppMessage, MeterValuesConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<MeterValuesConfirmation>>()
    }

    "serialize MeterValues response" {
        val confirmation = MeterValuesConfirmation
        val message = Message.Response("res-1", messageSerializer.toPayload(confirmation))
        message.toJsonString(messageSerializer) shouldBe "[3,\"res-1\",{}]"
    }
})
