package co.statu.rule.plugins.payment.db.dao

import co.statu.rule.database.Dao
import co.statu.rule.plugins.payment.db.model.Purchase
import co.statu.rule.plugins.payment.db.model.PurchaseStatus
import io.vertx.core.json.JsonObject
import io.vertx.jdbcclient.JDBCPool
import java.util.*

abstract class PurchaseDao : Dao<Purchase>(Purchase::class) {
    abstract suspend fun add(purchase: Purchase, jdbcPool: JDBCPool): UUID

    abstract suspend fun updateStatusById(
        id: UUID,
        status: PurchaseStatus,
        statusMessage: String?,
        jdbcPool: JDBCPool
    )

    abstract suspend fun updateClaimsById(
        id: UUID,
        claims: JsonObject,
        jdbcPool: JDBCPool
    )

    abstract suspend fun existsByIdUserIdAndStatus(
        id: UUID,
        userId: UUID,
        purchaseStatus: PurchaseStatus,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun countByUserId(
        userId: UUID,
        jdbcPool: JDBCPool
    ): Long

    abstract suspend fun countOfPaidByUserId(
        userId: UUID,
        jdbcPool: JDBCPool
    ): Long

    abstract suspend fun byUserId(
        userId: UUID,
        page: Long,
        jdbcPool: JDBCPool
    ): List<Purchase>

    abstract suspend fun expireExpiredPurchases(
        jdbcPool: JDBCPool
    )
}