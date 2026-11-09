package com.pushpyns.domain.validation

import com.pushpyns.domain.money.Money

enum class PynFieldError {
    NAME_REQUIRED,
    LATITUDE,
    LONGITUDE,
    LAT_LNG_PAIR,
}

enum class WorkOrderFieldError {
    JOB_REQUIRED,
    CUSTOMER_REQUIRED,
    LATITUDE,
    LONGITUDE,
    LAT_LNG_PAIR,
    LINE_ITEM,
}

data class LineItemInput(val description: String, val quantity: String, val unitPrice: String)

fun validatePyn(name: String, latitude: String, longitude: String): Set<PynFieldError> {
    val errors = mutableSetOf<PynFieldError>()
    if (name.trim().isEmpty()) errors += PynFieldError.NAME_REQUIRED
    errors +=
        coordinateErrors(latitude, longitude).map {
            when (it) {
                CoordError.LATITUDE -> PynFieldError.LATITUDE
                CoordError.LONGITUDE -> PynFieldError.LONGITUDE
                CoordError.PAIR -> PynFieldError.LAT_LNG_PAIR
            }
        }
    return errors
}

fun validateWorkOrder(
    jobName: String,
    customer: String,
    latitude: String,
    longitude: String,
    lineItems: List<LineItemInput>,
): Set<WorkOrderFieldError> {
    val errors = mutableSetOf<WorkOrderFieldError>()
    if (jobName.trim().isEmpty()) errors += WorkOrderFieldError.JOB_REQUIRED
    if (customer.trim().isEmpty()) errors += WorkOrderFieldError.CUSTOMER_REQUIRED
    errors +=
        coordinateErrors(latitude, longitude).map {
            when (it) {
                CoordError.LATITUDE -> WorkOrderFieldError.LATITUDE
                CoordError.LONGITUDE -> WorkOrderFieldError.LONGITUDE
                CoordError.PAIR -> WorkOrderFieldError.LAT_LNG_PAIR
            }
        }
    val anyInvalidLine =
        lineItems.any { item ->
            val touched = item.description.isNotBlank() || item.quantity.isNotBlank() || item.unitPrice.isNotBlank()
            touched && parsedLine(item) == null
        }
    if (anyInvalidLine) errors += WorkOrderFieldError.LINE_ITEM
    return errors
}

data class ParsedLine(val description: String, val quantity: String, val unitPriceCents: Long)

fun parsedLine(item: LineItemInput): ParsedLine? {
    val description = item.description.trim()
    val quantity = item.quantity.trim()
    val qty = quantity.toBigDecimalOrNull()
    val price = Money.parseDollars(item.unitPrice)
    if (description.isEmpty() || qty == null || qty.signum() <= 0 || price == null || price < 0) {
        return null
    }
    return ParsedLine(description, qty.stripTrailingZeros().toPlainString(), price)
}

private enum class CoordError { LATITUDE, LONGITUDE, PAIR }

private fun coordinateErrors(latitude: String, longitude: String): Set<CoordError> {
    val errors = mutableSetOf<CoordError>()
    val latRaw = latitude.trim()
    val lngRaw = longitude.trim()
    if (latRaw.isEmpty() != lngRaw.isEmpty()) errors += CoordError.PAIR
    val lat = latRaw.toDoubleOrNull()
    val lng = lngRaw.toDoubleOrNull()
    if (latRaw.isNotEmpty() && (lat == null || lat !in -90.0..90.0)) errors += CoordError.LATITUDE
    if (lngRaw.isNotEmpty() && (lng == null || lng !in -180.0..180.0)) errors += CoordError.LONGITUDE
    return errors
}

fun coordinateOrNull(text: String): Double? = text.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
