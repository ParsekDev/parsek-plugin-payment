package co.statu.rule.plugins.payment.api

import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.PaymentSystem
import co.statu.rule.plugins.payment.db.dao.PurchaseDao
import co.statu.rule.plugins.payment.db.impl.PurchaseDaoImpl
import co.statu.rule.plugins.payment.db.model.PurchaseStatus
import co.statu.rule.plugins.payment.error.InvalidPurchase
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.*

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class PaymentCallbackHandler(private val paymentPlugin: PaymentPlugin) {
    private val databaseManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val paymentSystem by lazy {
        paymentPlugin.pluginBeanContext.getBean(PaymentSystem::class.java)
    }

    private val purchaseDao: PurchaseDao = PurchaseDaoImpl()

    private val jdbcPool by lazy {
        databaseManager.getConnectionPool()
    }

    suspend fun validatePurchase(purchaseId: UUID, userId: UUID, purchaseStatus: PurchaseStatus) {
        val exists = purchaseDao.existsByIdUserIdAndStatus(purchaseId, userId, purchaseStatus, jdbcPool)

        if (!exists) {
            throw InvalidPurchase()
        }
    }

    suspend fun orderCreated(
        purchaseId: UUID,
        receiptUrl: String,
        createdAt: Long?,
        updatedAt: Long?,
        renewsAt: Long?,
        endsAt: Long?,
        taxAsLong: Long?
    ) {
        val purchase = purchaseDao.byId(purchaseId, jdbcPool)!!

        val typeListener = paymentSystem.getTypeListener(purchase.type)!!

        typeListener.onOrderCreated(purchase)

        val claims = purchase.claims

        claims.put("createdAt", createdAt)
        claims.put("updatedAt", updatedAt)
        claims.put("renewsAt", renewsAt)
        claims.put("endsAt", endsAt)
        claims.put("externalReceiptUrl", receiptUrl)
        claims.put("tax", taxAsLong)

        purchaseDao.updateClaimsById(purchaseId, claims, jdbcPool)

        purchaseDao.updateStatusById(purchaseId, PurchaseStatus.SUCCESS, null, jdbcPool)
    }

    suspend fun orderRefunded(purchaseId: UUID, externalOrderId: Long) {
        val purchase = purchaseDao.byId(purchaseId, jdbcPool)!!

        val typeListener = paymentSystem.getTypeListener(purchase.type)!!

        typeListener.onOrderRefunded(purchase, externalOrderId)

        purchaseDao.updateStatusById(purchaseId, PurchaseStatus.REFUNDED, null, jdbcPool)
    }
}