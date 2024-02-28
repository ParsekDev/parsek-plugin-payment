package co.statu.rule.plugins.payment.api

import co.statu.parsek.model.Successful
import co.statu.parsek.util.TextUtil.convertToSnakeCase
import co.statu.rule.auth.db.model.User
import co.statu.rule.plugins.payment.db.model.Purchase

interface TypeListener {
    fun getTypeName() = javaClass.simpleName.replace("PaymentListener", "").convertToSnakeCase().uppercase()

    suspend fun onHandleCheckout(user: User, value: String, checkout: Checkout): Successful

    suspend fun onOrderCreated(purchase: Purchase)

    suspend fun onOrderRefunded(purchase: Purchase, externalOrderId: Long)
}