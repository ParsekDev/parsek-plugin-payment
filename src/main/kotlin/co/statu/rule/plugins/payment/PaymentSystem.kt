package co.statu.rule.plugins.payment

import co.statu.parsek.PluginEventManager
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.api.PaymentCallbackHandler
import co.statu.rule.plugins.payment.api.PaymentMethodIntegration
import co.statu.rule.plugins.payment.api.TypeListener
import co.statu.rule.plugins.payment.db.dao.PurchaseDao
import co.statu.rule.plugins.payment.db.impl.PurchaseDaoImpl
import co.statu.rule.plugins.payment.event.PaymentEventListener
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Lazy
class PaymentSystem(
    private val paymentPlugin: PaymentPlugin,
    private val vertx: Vertx,
    private val logger: Logger
) {
    private val databaseManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    val paymentCallbackHandler: PaymentCallbackHandler by lazy {
        paymentPlugin.pluginBeanContext.getBean(PaymentCallbackHandler::class.java)
    }

    private val typeListeners = mutableListOf<TypeListener>()
    private val paymentMethodIntegrations = mutableListOf<PaymentMethodIntegration>()
    private var schedulerId: Long? = null

    private val purchaseDao: PurchaseDao = PurchaseDaoImpl()

    init {
        val paymentEventHandlers = PluginEventManager.getEventListeners<PaymentEventListener>()

        paymentEventHandlers.forEach {
            it.onPaymentSystemInit(this)
        }
    }

    fun register(typeListener: TypeListener) {
        typeListeners.add(typeListener)
    }

    fun register(methodIntegration: PaymentMethodIntegration) {
        paymentMethodIntegrations.add(methodIntegration)
    }

    internal fun getTypes() = typeListeners

    internal fun getTypeListener(type: String) = typeListeners.find { it.getTypeName() == type }

    internal fun getPaymentMethodIntegration(name: String) = paymentMethodIntegrations.find { it.getName() == name }

    internal fun start() {
        stop()

        failExpiredPurchases()

        schedulerId = vertx.setPeriodic(60 * 1000) { // every minute
            failExpiredPurchases()
        }

        logger.info("Purchase expire scheduler started")
    }

    private fun failExpiredPurchases() {
        val jdbcPool = databaseManager.getConnectionPool()

        CoroutineScope(vertx.dispatcher()).launch {
            purchaseDao.expireExpiredPurchases(jdbcPool)
        }
    }

    internal fun stop() {
        schedulerId?.let {
            vertx.cancelTimer(it)

            logger.info("Purchase expire scheduler stopped")
        }
    }
}