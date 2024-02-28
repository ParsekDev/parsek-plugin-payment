package co.statu.rule.plugins.payment.event

import co.statu.rule.database.DatabaseManager
import co.statu.rule.database.event.DatabaseEventListener
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.PaymentSystem

class DatabaseEventHandler : DatabaseEventListener {
    override suspend fun onReady(databaseManager: DatabaseManager) {
        databaseManager.migrateNewPluginId("payment", PaymentPlugin.INSTANCE.context.pluginId, PaymentPlugin.INSTANCE)
        databaseManager.initialize(PaymentPlugin.INSTANCE, PaymentPlugin.tables, PaymentPlugin.migrations)

        PaymentPlugin.databaseManager = databaseManager

        PaymentPlugin.paymentSystem = PaymentSystem.create(
            PaymentPlugin.databaseManager, PaymentPlugin.INSTANCE.context.vertx,
            PaymentPlugin.logger
        )

        PaymentPlugin.paymentSystem.start()
    }
}