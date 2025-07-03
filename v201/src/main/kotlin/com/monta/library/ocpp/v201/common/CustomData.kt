package com.monta.library.ocpp.v201.common

class CustomData : HashMap<String, String?>() {

    val vendorId: String
        get() = requireNotNull(get("vendorId"))
}
