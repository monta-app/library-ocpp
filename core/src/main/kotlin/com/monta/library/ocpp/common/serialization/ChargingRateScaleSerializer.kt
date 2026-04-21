package com.monta.library.ocpp.common.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.math.BigDecimal
import java.math.RoundingMode

class OneDecimalFloorSerializer : StdSerializer<Double>(Double::class.java) {
    override fun serialize(value: Double, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeNumber(BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toDouble())
    }
}

class OneDecimalFloorDeserializer : StdDeserializer<Double>(Double::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Double {
        return BigDecimal(p.doubleValue).setScale(1, RoundingMode.HALF_UP).toDouble()
    }
}
