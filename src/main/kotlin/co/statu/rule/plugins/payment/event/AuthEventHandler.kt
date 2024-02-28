package co.statu.rule.plugins.payment.event

import co.statu.rule.auth.event.AuthEventListener
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.plugins.payment.PaymentPlugin

class AuthEventHandler : AuthEventListener {
    override suspend fun onReady(authProvider: AuthProvider) {
        PaymentPlugin.authProvider = authProvider
    }
}