package com.monta.library.ocpp.common.session

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OcppVersionTest : StringSpec({

    "simple versions matches" {
        OcppSession.OcppVersion.valueOf("ocpp1.6", OcppSession.OcppVersion.V2_0_1) shouldBe OcppSession.OcppVersion.V1_6
        OcppSession.OcppVersion.valueOf("ocpp2.0.1", OcppSession.OcppVersion.V1_6) shouldBe OcppSession.OcppVersion.V2_0_1
    }

    "empty value gives default" {
        OcppSession.OcppVersion.valueOf(null, OcppSession.OcppVersion.V2_0_1) shouldBe OcppSession.OcppVersion.V2_0_1
        OcppSession.OcppVersion.valueOf("  ", OcppSession.OcppVersion.V1_6) shouldBe OcppSession.OcppVersion.V1_6
    }

    "first match is preferred" {
        OcppSession.OcppVersion.valueOf("ocpp1.6,ocpp2.0.1", OcppSession.OcppVersion.V2_0_1) shouldBe OcppSession.OcppVersion.V1_6
        OcppSession.OcppVersion.valueOf("ocpp2.0.1,ocpp1.6", OcppSession.OcppVersion.V1_6) shouldBe OcppSession.OcppVersion.V2_0_1
        OcppSession.OcppVersion.valueOf("ocpp2.1,ocpp2.0.1,ocpp1.6", OcppSession.OcppVersion.V1_6) shouldBe OcppSession.OcppVersion.V2_0_1
    }

    "format with spaces is accepted" {
        OcppSession.OcppVersion.valueOf(" ocpp1.6 , ocpp2.0.1 ", OcppSession.OcppVersion.V2_0_1) shouldBe OcppSession.OcppVersion.V1_6
    }

    "not found throws exception" {
        shouldThrow<IllegalArgumentException> {
            OcppSession.OcppVersion.valueOf("ocpp1.5", OcppSession.OcppVersion.V2_0_1)
        }
    }

    "illegal format throws exception" {
        shouldThrow<IllegalArgumentException> {
            OcppSession.OcppVersion.valueOf(",", OcppSession.OcppVersion.V2_0_1)
        }
    }

    "an entirely different protocol throws exception" {
        shouldThrow<IllegalArgumentException> {
            OcppSession.OcppVersion.valueOf("anEntirelyDifferentProtocol", OcppSession.OcppVersion.V2_0_1)
        }
    }
    "valueOf never resolves OCPP 2.1 (opt-in via negotiate only)" {
        shouldThrow<IllegalArgumentException> {
            OcppSession.OcppVersion.valueOf("ocpp2.1", OcppSession.OcppVersion.V1_6)
        }
        OcppSession.OcppVersion.valueOf("ocpp2.1, ocpp1.6", OcppSession.OcppVersion.V2_0_1) shouldBe OcppSession.OcppVersion.V1_6
    }

    "negotiate honours the charging station's order" {
        val all = OcppSession.OcppVersion.entries.toSet()
        OcppSession.OcppVersion.negotiate("ocpp2.1, ocpp2.0.1, ocpp1.6", all) shouldBe OcppSession.OcppVersion.V2_1
        OcppSession.OcppVersion.negotiate("ocpp1.6,ocpp2.1", all) shouldBe OcppSession.OcppVersion.V1_6
        OcppSession.OcppVersion.negotiate("ocpp2.0.1", all) shouldBe OcppSession.OcppVersion.V2_0_1
        OcppSession.OcppVersion.negotiate("ocpp1.5, ocpp2.1", all) shouldBe OcppSession.OcppVersion.V2_1
    }

    "negotiate only returns supported versions" {
        val legacy = setOf(OcppSession.OcppVersion.V1_6, OcppSession.OcppVersion.V2_0_1)
        OcppSession.OcppVersion.negotiate("ocpp2.1, ocpp2.0.1", legacy) shouldBe OcppSession.OcppVersion.V2_0_1
        OcppSession.OcppVersion.negotiate("ocpp2.1", legacy) shouldBe null
    }

    "negotiate returns null for absent or unknown protocols" {
        val all = OcppSession.OcppVersion.entries.toSet()
        OcppSession.OcppVersion.negotiate(null, all) shouldBe null
        OcppSession.OcppVersion.negotiate(" ", all) shouldBe null
        OcppSession.OcppVersion.negotiate("ocpp1.5,soap", all) shouldBe null
    }
})
