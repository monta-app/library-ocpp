package com.monta.library.ocpp.common.serialization

import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.ser.std.StdSerializer
import java.math.BigDecimal
import java.math.RoundingMode

class OneDecimalFloorSerializer : StdSerializer<Double>(Double::class.java) {
    override fun serialize(value: Double, gen: JsonGenerator, provider: SerializationContext) {
        gen.writeNumber(BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toDouble())
    }
}

class OneDecimalFloorDeserializer : StdDeserializer<Double>(Double::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Double {
        return BigDecimal(p.doubleValue).setScale(1, RoundingMode.HALF_UP).toDouble()
    }
}
