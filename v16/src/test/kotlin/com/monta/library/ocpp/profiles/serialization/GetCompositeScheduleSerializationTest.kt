package com.monta.library.ocpp.profiles.serialization

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import com.monta.library.ocpp.v16.smartcharge.ChargingSchedule
import com.monta.library.ocpp.v16.smartcharge.ChargingSchedulePeriod
import com.monta.library.ocpp.v16.smartcharge.GetCompositeScheduleConfirmation
import com.monta.library.ocpp.v16.smartcharge.GetCompositeScheduleStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GetCompositeScheduleSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse a valid get composite schedule response" {
        val jsonString = TestUtils.getFileAsString("smartcharge/get-composite-schedule-resp.json")
        val parsingResult = messageSerializer.parse(jsonString)

        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value

        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "d16cd067-9506-441e-902b-af1b9b15b8e4"
        ocppMessage.payload shouldBe TestUtils.toJsonNode("{\"status\":\"Accepted\",\"connectorId\":1,\"scheduleStart\":\"2025-05-22T08:29:27.000Z\",\"chargingSchedule\":{\"duration\":60,\"chargingRateUnit\":\"A\",\"chargingSchedulePeriod\":[{\"startPeriod\":0,\"limit\":32.123,\"numberPhases\":1}],\"minChargingRate\":0.1234}}")

        val response = messageSerializer.deserializePayload(ocppMessage, GetCompositeScheduleConfirmation::class.java)
        response.shouldBeInstanceOf<ParsingResult.Success<GetCompositeScheduleConfirmation>>()
        val payload = response.value
        payload.status shouldBe GetCompositeScheduleStatus.Accepted
        payload.connectorId shouldBe 1
        payload.scheduleStart.toString() shouldBe "2025-05-22T08:29:27Z"
        payload.chargingSchedule shouldNotBeNull {
            chargingRateUnit shouldBe ChargingRateUnit.A
            duration shouldBe 60
            minChargingRate shouldBe 0.1
            chargingSchedulePeriod shouldNotBeNull {
                size shouldBe 1
                this[0].startPeriod shouldBe 0
                this[0].limit shouldBe 32.1
                this[0].numberPhases shouldBe 1
            }
        }
    }

    "serialize a get composite schedule response and floor limit and minChargingRate to 1 decimal" {
        val confirmation = GetCompositeScheduleConfirmation(
            status = GetCompositeScheduleStatus.Accepted,
            connectorId = 1,
            chargingSchedule = ChargingSchedule(
                duration = 60,
                chargingRateUnit = ChargingRateUnit.A,
                chargingSchedulePeriod = listOf(
                    ChargingSchedulePeriod(
                        startPeriod = 0,
                        limit = 32.123,
                        numberPhases = 1
                    )
                ),
                minChargingRate = 15.678
            )
        )

        messageSerializer.toPayloadString(confirmation) shouldBe
            """{"status":"Accepted","connectorId":1,"chargingSchedule":{"duration":60,"chargingRateUnit":"A","chargingSchedulePeriod":[{"startPeriod":0,"limit":32.1,"numberPhases":1}],"minChargingRate":15.7}}"""
    }
})
