package co.statu.rule.plugins.payment.api

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.config.ConfigManager.Companion.putAll
import co.statu.rule.auth.db.model.User
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.PaymentConfig
import co.statu.rule.plugins.payment.db.dao.PurchaseDao
import co.statu.rule.plugins.payment.db.impl.PurchaseDaoImpl
import co.statu.rule.plugins.payment.db.model.BillDetail
import co.statu.rule.plugins.payment.db.model.Purchase
import co.statu.rule.plugins.payment.db.model.PurchaseStatus
import io.vertx.core.json.JsonObject
import java.util.*

class Checkout internal constructor(
    private val user: User,
    private val type: String,
    private val pluginConfigManager: PluginConfigManager<PaymentConfig>,
    private val databaseManager: DatabaseManager,
    val billDetail: BillDetail,
    val typeListener: TypeListener,
    val methodIntegration: PaymentMethodIntegration,
) {
    private val purchaseDao: PurchaseDao = PurchaseDaoImpl()

    private val paymentConfig by lazy {
        pluginConfigManager.config
    }

    private val jdbcPool by lazy {
        databaseManager.getConnectionPool()
    }

    val expireDate by lazy {
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.SECOND, paymentConfig.expireInSeconds)

        calendar.timeInMillis
    }

    suspend fun savePurchase(amount: Long, price: Long, claims: JsonObject = JsonObject()): UUID {
        val purchaseClaims = JsonObject()

        purchaseClaims.putAll(claims.map)

        val purchase = Purchase(
            userId = user.id,
            amount = amount,
            type = type,
            price = price,
            claims = purchaseClaims,
            status = PurchaseStatus.PENDING,
            expiresAt = expireDate,
            billDetail = JsonObject.mapFrom(billDetail)
        )

        val purchaseId = purchaseDao.add(purchase, jdbcPool)

        return purchaseId
    }

    fun isTestMode() = paymentConfig.testMode

    suspend fun failPurchase(purchaseId: UUID, statusMessage: String?) {
        purchaseDao.updateStatusById(purchaseId, PurchaseStatus.FAILED, statusMessage, jdbcPool)
    }

    suspend fun updateClaims(purchaseId: UUID, claims: JsonObject) {
        purchaseDao.updateClaimsById(purchaseId, claims, jdbcPool)
    }
}