package com.monta.library.ocpp.v21

import com.monta.library.ocpp.common.profile.NoConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import com.monta.library.ocpp.common.profile.OcppUnconfirmedRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.NotifyPeriodicEventStreamRequest
import com.monta.library.ocpp.v21.blocks.diagnostics.StreamDataElement
import com.monta.library.ocpp.v21.blocks.provisioning.BootNotificationRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigDecimal
import java.time.ZonedDateTime

class V21ActionsTest : StringSpec({

    "registry holds all 91 OCPP 2.1 actions" {
        V21Actions.all shouldHaveSize 91
        V21Actions.all.count { it.direction == V21Direction.CSMS_TO_CS } shouldBe 54
        V21Actions.all.count { it.direction == V21Direction.CS_TO_CSMS } shouldBe 35
        V21Actions.all.count { it.direction == V21Direction.BOTH } shouldBe 1
        V21Actions.all.count { it.direction == V21Direction.SEND } shouldBe 1
        V21Actions.all.count { it.sentByCsms } shouldBe 55
        V21Actions.all.count { it.sentByChargingStation } shouldBe 37
    }

    "action names, slugs and classes are consistent" {
        V21Actions.all.map { it.action }.toSet() shouldHaveSize 91
        V21Actions.all.map { it.slug }.toSet() shouldHaveSize 91
        V21Actions.all.forEach { action ->
            action.feature.name shouldBe action.action
            action.feature.requestType.simpleName shouldBe "${action.action}Request"
            V21Actions.byAction(action.action) shouldBe action
            V21Actions.bySlug(action.slug) shouldBe action
            V21Actions.byRequestType(action.feature.requestType) shouldBe action
            if (action.direction == V21Direction.SEND) {
                action.feature.confirmationType shouldBe NoConfirmation::class.java
            } else {
                action.feature.confirmationType.simpleName shouldBe "${action.action}Response"
            }
        }
    }

    "slugs follow the kebab-case convention" {
        V21Actions.byAction("Get15118EVCertificate")?.slug shouldBe "get-15118-ev-certificate"
        V21Actions.byAction("GetInstalledCertificateIds")?.slug shouldBe "get-installed-certificate-ids"
        V21Actions.byAction("AFRRSignal")?.slug shouldBe "afrr-signal"
        V21Actions.byAction("SetDERControl")?.slug shouldBe "set-der-control"
    }

    "NotifyPeriodicEventStream is an unconfirmed SEND action" {
        val request: OcppRequest = NotifyPeriodicEventStreamRequest(
            id = 1,
            pending = 0,
            basetime = ZonedDateTime.now(),
            data = listOf(StreamDataElement(t = BigDecimal("1.5"), v = "230.4"))
        )
        request.shouldBeInstanceOf<OcppUnconfirmedRequest>()
        request.actionName() shouldBe "NotifyPeriodicEventStream"
        BootNotificationRequest::class.java.simpleName shouldBe "BootNotificationRequest"
    }
})
