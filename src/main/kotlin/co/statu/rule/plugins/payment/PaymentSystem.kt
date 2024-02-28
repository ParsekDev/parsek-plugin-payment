package co.statu.rule.plugins.payment

import co.statu.rule.database.Dao.Companion.get
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.api.PaymentCallbackHandler
import co.statu.rule.plugins.payment.api.PaymentMethodIntegration
import co.statu.rule.plugins.payment.api.TypeListener
import co.statu.rule.plugins.payment.db.dao.PurchaseDao
import co.statu.rule.plugins.payment.event.PaymentEventListener
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger

class PaymentSystem private constructor(
    private val databaseManager: DatabaseManager,
    private val vertx: Vertx,
    private val logger: Logger
) {
    companion object {
        internal fun create(databaseManager: DatabaseManager, vertx: Vertx, logger: Logger) =
            PaymentSystem(databaseManager, vertx, logger)
    }

    private val typeListeners = mutableListOf<TypeListener>()
    private val paymentMethodIntegrations = mutableListOf<PaymentMethodIntegration>()
    private var schedulerId: Long? = null

    private val purchaseDao by lazy {
        get<PurchaseDao>(PaymentPlugin.tables)
    }

    val paymentCallbackHandler by lazy {
        PaymentCallbackHandler()
    }

    init {
        val paymentEventHandlers =
            PaymentPlugin.INSTANCE.context.pluginEventManager.getEventHandlers<PaymentEventListener>()

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