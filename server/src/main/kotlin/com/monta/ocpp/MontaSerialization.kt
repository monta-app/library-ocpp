package com.monta.ocpp

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

object MontaSerialization {

    fun getDefaultMapper(
        serializeNulls: Boolean = false
    ): ObjectMapper {
        return withDefaults(
            builder = JsonMapper.builder(),
            serializeNulls = serializeNulls
        ).build()
    }

    /**
     * Jackson 3 mappers are immutable, so configuration happens on the builder. java.time support is
     * built into databind now (no JavaTimeModule needed); we only register the Kotlin module here.
     */
    fun withDefaults(
        builder: JsonMapper.Builder,
        serializeNulls: Boolean = false
    ): JsonMapper.Builder {
        return builder
            .addModule(kotlinModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .changeDefaultPropertyInclusion {
                it.withValueInclusion(
                    if (serializeNulls) {
                        JsonInclude.Include.ALWAYS
                    } else {
                        JsonInclude.Include.NON_NULL
                    }
                )
            }
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .findAndAddModules()
    }
}
