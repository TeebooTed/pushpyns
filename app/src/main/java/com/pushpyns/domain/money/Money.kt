package com.pushpyns.domain.money

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object Money {
    fun parseDollars(text: String): Long? {
        val cleaned = text.trim().removePrefix("$").replace(",", "")
        if (cleaned.isEmpty()) return null
        val decimal = cleaned.toBigDecimalOrNull() ?: return null
        if (decimal.stripTrailingZeros().scale() > 2) return null
        return try {
            decimal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun formatCents(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val value = abs(cents)
        val dollars = value / 100
        val remainder = (value % 100).toString().padStart(2, '0')
        return "$sign$$dollars.$remainder"
    }

    fun lineTotalCents(quantity: String, unitPriceCents: Long): Long {
        val qty = quantity.toBigDecimalOrNull() ?: return 0
        if (qty.signum() <= 0 || unitPriceCents < 0) return 0
        return qty
            .multiply(BigDecimal.valueOf(unitPriceCents))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun displayQuantity(quantity: String): String = quantity.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: quantity
}
