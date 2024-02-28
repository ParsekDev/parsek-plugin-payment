package co.statu.rule.plugins.payment.api

import co.statu.parsek.util.TextUtil.convertToSnakeCase
import co.statu.rule.auth.db.model.User
import java.util.*

interface PaymentMethodIntegration {
    fun getName() = javaClass.simpleName.replace("PaymentIntegration", "").convertToSnakeCase().uppercase()

    suspend fun sendCheckoutRequest(
        user: User,
        purchaseId: UUID,
        amount: Long,
        price: Long,
        title: String,
        description: String,
        checkout: Checkout
    ): CheckoutResponse
}