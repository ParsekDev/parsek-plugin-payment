package co.statu.rule.plugins.payment.event

import co.statu.parsek.api.event.PluginEventListener
import co.statu.rule.plugins.payment.PaymentSystem

interface PaymentEventListener : PluginEventListener {
    fun onPaymentSystemInit(paymentSystem: PaymentSystem) {}
}