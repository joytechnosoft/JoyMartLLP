package com.joymart.llp.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Display-only INR formatter. Keeps API/database values unchanged. */
object MoneyFormatter {
    private val symbols = DecimalFormatSymbols(Locale("en", "IN")).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val pattern = DecimalFormat("#,##,##0.00", symbols).apply {
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = true
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    @Synchronized
    fun format(raw: String?): String {
        if (raw.isNullOrBlank()) return "0.00"
        val value = raw.trim().replace(",", "").toBigDecimalOrNull() ?: return "0.00"
        return pattern.format(value.setScale(2, RoundingMode.HALF_UP))
    }

    fun inr(raw: String?): String = "₹${format(raw)}"
}
