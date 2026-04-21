package com.monta.library.ocpp.v201.blocks.smartcharging

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v201.common.GenericStatus
import com.monta.library.ocpp.v201.common.chargingprofile.ChargingSchedulePeriod
import com.monta.library.ocpp.v201.error.OcppErrorResponderV201
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime

class GetCompositeScheduleSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_2, OcppErrorResponderV201)

    "parse a valid get composite schedule response and floor limit to 1 decimal" {
        val jsonString = TestUtils.getFileAsString("smartcharging/get-composite-schedule-resp.json")
        val parsingResult = messageSerializer.parse(jsonString)

        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val ocppMessage = parsingResult.value

        ocppMessage.shouldBeInstanceOf<Message.Response>()
        ocppMessage.uniqueId shouldBe "c3d4e5f6-a7b8-9012-cdef-123456789012"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(
            """{"status":"Accepted","schedule":{"evseId":1,"duration":3600,"scheduleStart":"2025-05-22T08:29:27.000Z","chargingRateUnit":"A","chargingSchedulePeriod":[{"startPeriod":0,"limit":32.123,"numberPhases":3},{"startPeriod":1800,"limit":20.567,"numberPhases":3}]}}"""
        )

        val response = messageSerializer.deserializePayload(ocppMessage, GetCompositeScheduleResponse::class.java)
        response.shouldBeInstanceOf<ParsingResult.Success<GetCompositeScheduleResponse>>()
        val payload = response.value
        payload.status shouldBe GenericStatus.Accepted
        payload.schedule shouldNotBeNull {
            evseId shouldBe 1L
            duration shouldBe 3600L
            chargingRateUnit shouldBe ChargingRateUnit.A
            chargingSchedulePeriod shouldNotBeNull {
                size shouldBe 2
                this[0].startPeriod shouldBe 0
                this[0].limit shouldBe 32.1
                this[0].numberPhases shouldBe 3
                this[1].startPeriod shouldBe 1800
                this[1].limit shouldBe 20.6
                this[1].numberPhases shouldBe 3
            }
        }
    }

    "serialize a get composite schedule response and floor limit to 1 decimal" {
        val response = GetCompositeScheduleResponse(
            status = GenericStatus.Accepted,
            schedule = GetCompositeScheduleResponse.CompositeSchedule(
                evseId = 1L,
                duration = 3600L,
                scheduleStart = ZonedDateTime.parse("2025-05-22T08:29:27Z"),
                chargingRateUnit = ChargingRateUnit.A,
                chargingSchedulePeriod = listOf(
                    ChargingSchedulePeriod(startPeriod = 0, limit = 32.123, numberPhases = 3),
                    ChargingSchedulePeriod(startPeriod = 1800, limit = 20.567, numberPhases = 3)
                )
            )
        )

        messageSerializer.toPayloadString(response) shouldBe
            """{"status":"Accepted","schedule":{"chargingSchedulePeriod":[{"startPeriod":0,"limit":32.1,"numberPhases":3},{"startPeriod":1800,"limit":20.6,"numberPhases":3}],"evseId":1,"duration":3600,"scheduleStart":"2025-05-22T08:29:27.000Z","chargingRateUnit":"A"}}"""
    }
})
