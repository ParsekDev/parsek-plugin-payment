package co.statu.rule.plugins.payment.api

import co.statu.parsek.model.Successful
import java.math.BigDecimal

class CheckoutResponse(price: BigDecimal, checkoutUrl: String) : Successful(
    mapOf(
        "price" to price,
        "checkoutUrl" to checkoutUrl
    )
)