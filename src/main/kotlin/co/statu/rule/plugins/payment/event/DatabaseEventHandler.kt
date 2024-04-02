package co.statu.rule.plugins.payment.event

import co.statu.parsek.api.annotation.EventListener
import co.statu.rule.database.DatabaseManager
import co.statu.rule.database.event.DatabaseEventListener
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.PaymentSystem

@EventListener
class DatabaseEventHandler(private val paymentPlugin: PaymentPlugin) : DatabaseEventListener {
    private val paymentSystem by lazy {
        paymentPlugin.pluginBeanContext.getBean(PaymentSystem::class.java)
    }

    override suspend fun onReady(databaseManager: DatabaseManager) {
        databaseManager.migrateNewPluginId("payment", paymentPlugin.pluginId, paymentPlugin)
        databaseManager.initialize(paymentPlugin, paymentPlugin)

        paymentSystem.start()
    }
}