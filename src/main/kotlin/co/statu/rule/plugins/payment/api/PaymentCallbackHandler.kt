package co.statu.rule.plugins.payment.api

import co.statu.rule.database.Dao.Companion.get
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.db.dao.PurchaseDao
import co.statu.rule.plugins.payment.db.model.PurchaseStatus
import co.statu.rule.plugins.payment.error.InvalidPurchase
import java.util.*

class PaymentCallbackHandler internal constructor() {
    private val purchaseDao by lazy {
        get<PurchaseDao>(PaymentPlugin.tables)
    }

    private val databaseManager by lazy {
        PaymentPlugin.databaseManager
    }

    private val jdbcPool by lazy {
        databaseManager.getConnectionPool()
    }

    private val paymentSystem by lazy {
        PaymentPlugin.paymentSystem
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