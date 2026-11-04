package io.radar.sdk

import io.radar.sdk.model.RadarAddress

/**
 * The fields `/v1/addresses/validate` takes. The API accepts a street and number or an address label, but not both.
 */
internal data class RadarValidateAddressRequest(
    val line: Line,
    val unit: String?,
    val city: String,
    val stateCode: String,
    val postalCode: String,
    val countryCode: String
) {

    sealed class Line {
        data class StreetAndNumber(val street: String, val number: String) : Line()
        data class Label(val label: String) : Line()
    }

    companion object {

        /**
         * Uses the address's street and number when both are set, and its label otherwise. Addresses from search and
         * geocoding have all three.
         */
        fun from(address: RadarAddress): RadarValidateAddressRequest {
            // RadarAddress.fromJson reads a missing field as "" and a JSON null as "null", so treat both as missing
            fun String?.presentOrEmpty() = this?.takeUnless { it == "null" } ?: ""

            val street = address.street.presentOrEmpty()
            val number = address.number.presentOrEmpty()
            val line = if (street.isNotEmpty() && number.isNotEmpty()) {
                Line.StreetAndNumber(street, number)
            } else {
                Line.Label(address.addressLabel.presentOrEmpty())
            }
            return RadarValidateAddressRequest(
                line = line,
                unit = address.unit.presentOrEmpty(),
                city = address.city.presentOrEmpty(),
                stateCode = address.stateCode.presentOrEmpty(),
                postalCode = address.postalCode.presentOrEmpty(),
                countryCode = address.countryCode.presentOrEmpty()
            )
        }
    }
}
