package co.statu.rule.plugins.payment.util

import java.math.BigDecimal

object TextUtil {
    fun Long.toCurrencyFormat() = BigDecimal(this).movePointLeft(2)
}