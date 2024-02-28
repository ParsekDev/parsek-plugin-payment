package co.statu.rule.plugins.payment.event

import co.statu.parsek.api.PluginEvent
import co.statu.rule.plugins.payment.PaymentSystem

interface PaymentEventListener : PluginEvent {
    fun onPaymentSystemInit(paymentSystem: PaymentSystem) {}
}