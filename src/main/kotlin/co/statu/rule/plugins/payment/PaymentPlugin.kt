package co.statu.rule.plugins.payment

import co.statu.parsek.api.ParsekPlugin
import co.statu.parsek.api.PluginContext
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.Dao
import co.statu.rule.database.DatabaseManager
import co.statu.rule.database.DatabaseMigration
import co.statu.rule.plugins.payment.db.impl.BillDetailDaoImpl
import co.statu.rule.plugins.payment.db.impl.PurchaseDaoImpl
import co.statu.rule.plugins.payment.db.migration.DbMigration1To2
import co.statu.rule.plugins.payment.event.AuthEventHandler
import co.statu.rule.plugins.payment.event.DatabaseEventHandler
import co.statu.rule.plugins.payment.event.ParsekEventHandler
import co.statu.rule.plugins.payment.event.RouterEventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PaymentPlugin(pluginContext: PluginContext) : ParsekPlugin(pluginContext) {
    companion object {
        internal val logger: Logger = LoggerFactory.getLogger(PaymentPlugin::class.java)

        internal lateinit var pluginConfigManager: PluginConfigManager<PaymentConfig>

        internal lateinit var INSTANCE: PaymentPlugin

        internal lateinit var databaseManager: DatabaseManager

        internal lateinit var authProvider: AuthProvider

        internal val tables by lazy {
            mutableListOf<Dao<*>>(
                PurchaseDaoImpl(),
                BillDetailDaoImpl()
            )
        }

        internal val migrations by lazy {
            listOf<DatabaseMigration>(
                DbMigration1To2()
            )
        }

        internal lateinit var paymentSystem: PaymentSystem
    }

    init {
        INSTANCE = this

        logger.info("Initialized instance")

        context.pluginEventManager.register(this, DatabaseEventHandler())
        context.pluginEventManager.register(this, AuthEventHandler())
        context.pluginEventManager.register(this, RouterEventHandler())
        context.pluginEventManager.register(this, ParsekEventHandler())

        logger.info("Registered events")
    }
}