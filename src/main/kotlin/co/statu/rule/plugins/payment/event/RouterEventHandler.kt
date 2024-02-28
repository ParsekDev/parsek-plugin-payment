package co.statu.rule.plugins.payment.event

import co.statu.parsek.api.event.RouterEventListener
import co.statu.parsek.model.Route
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.route.CheckoutAPI
import co.statu.rule.plugins.payment.route.GetPurchasesAPI
import co.statu.rule.plugins.payment.route.billdetail.*

class RouterEventHandler : RouterEventListener {
    override fun onInitRouteList(routes: MutableList<Route>) {
        val databaseManager = PaymentPlugin.databaseManager
        val authProvider = PaymentPlugin.authProvider
        val paymentSystem = PaymentPlugin.paymentSystem
        val pluginConfigManager = PaymentPlugin.pluginConfigManager

        routes.addAll(
            listOf(
                CheckoutAPI(authProvider, paymentSystem, pluginConfigManager, databaseManager),
                GetBillDetailsAPI(databaseManager, authProvider),
                AddBillDetailAPI(authProvider, databaseManager),
                GetBillDetailAPI(databaseManager, authProvider),
                DeleteBillDetailAPI(databaseManager, authProvider),
                UpdateBillDetailAPI(authProvider, databaseManager),
                GetPurchasesAPI(databaseManager, authProvider)
            )
        )
    }
}