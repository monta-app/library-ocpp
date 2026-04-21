package com.monta.library.ocpp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object TestUtils {
    private val objectMapper = jacksonObjectMapper()

    fun getFileAsString(filename: String): String {
        return requireNotNull(object {}.javaClass.classLoader.getResource(filename)).readText()
    }

    fun toJsonNode(value: String): JsonNode {
        return objectMapper.readTree(value)
    }
}
