package co.statu.rule.plugins.payment.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.plugins.payment.db.dao.PurchaseDao
import co.statu.rule.plugins.payment.db.model.Purchase
import co.statu.rule.plugins.payment.db.model.PurchaseStatus
import io.vertx.core.json.JsonObject
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.*

class PurchaseDaoImpl : PurchaseDao() {

    override suspend fun init(jdbcPool: JDBCPool, plugin: ParsekPlugin) {
        jdbcPool
            .query(
                """
                        CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                            `id` UUID NOT NULL,
                            `userId` UUID NOT NULL,
                            `amount` Int64 DEFAULT 1,
                            `type` String NOT NULL,
                            `price` Int64 NOT NULL,
                            `claims` String DEFAULT '{}',
                            `status` String NOT NULL,
                            `statusMessage` Nullable(String),
                            `billDetail` String NOT NULL,
                            `createdAt` Int64 NOT NULL,
                            `updatedAt` Int64 NOT NULL,
                            `expiresAt` Int64 NOT NULL
                        ) ENGINE = MergeTree() order by `createdAt`;
                        """
            )
            .execute()
            .await()
    }

    override suspend fun add(purchase: Purchase, jdbcPool: JDBCPool): UUID {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (${fields.toTableQuery()}) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    purchase.id,
                    purchase.userId,
                    purchase.amount,
                    purchase.type,
                    purchase.price,
                    purchase.claims.encode(),
                    purchase.status,
                    purchase.statusMessage,
                    purchase.billDetail.encode(),
                    purchase.createdAt,
                    purchase.updatedAt,
                    purchase.expiresAt
                )
            )
            .await()

        return purchase.id
    }

    override suspend fun updateStatusById(
        id: UUID,
        status: PurchaseStatus,
        statusMessage: String?,
        jdbcPool: JDBCPool
    ) {
        val query =
            "ALTER TABLE `${getTablePrefix() + tableName}` UPDATE `status` = ?, `statusMessage` = ?, `updatedAt` = ? WHERE `id` = ?;"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    status.name,
                    statusMessage,
                    System.currentTimeMillis(),
                    id
                )
            )
            .await()
    }

    override suspend fun updateClaimsById(
        id: UUID,
        claims: JsonObject,
        jdbcPool: JDBCPool
    ) {
        val query =
            "ALTER TABLE `${getTablePrefix() + tableName}` UPDATE `claims` = ?, `updatedAt` = ? WHERE `id` = ?;"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    claims.encode(),
                    System.currentTimeMillis(),
                    id
                )
            )
            .await()
    }

    override suspend fun existsByIdUserIdAndStatus(
        id: UUID,
        userId: UUID,
        purchaseStatus: PurchaseStatus,
        jdbcPool: JDBCPool
    ): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` where `id` = ? and `userId` = ? and `status` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(id, userId, purchaseStatus.name))
            .await()

        return rows.toList()[0].getLong(0) == 1L
    }

    override suspend fun countByUserId(
        userId: UUID,
        jdbcPool: JDBCPool
    ): Long {
        val query =
            "SELECT COUNT(*) FROM `${getTablePrefix() + tableName}` WHERE `userId` = ? AND `status` != ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId, PurchaseStatus.EXPIRED.name))
            .await()

        return rows.toList()[0].getLong(0)
    }

    override suspend fun countOfPaidByUserId(
        userId: UUID,
        jdbcPool: JDBCPool
    ): Long {
        val query =
            "SELECT COUNT(*) FROM `${getTablePrefix() + tableName}` WHERE `userId` = ? AND `status` = ? AND `price` > 0"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId, PurchaseStatus.SUCCESS.name))
            .await()

        return rows.toList()[0].getLong(0)
    }

    override suspend fun byUserId(
        userId: UUID,
        page: Long,
        jdbcPool: JDBCPool
    ): List<Purchase> {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `userId` = ? AND `status` != ? ORDER BY `createdAt` DESC LIMIT 10 ${if (page == 1L) "" else "OFFSET ${(page - 1) * 10}"}"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId, PurchaseStatus.EXPIRED.name))
            .await()

        return rows.toEntities()
    }

    override suspend fun expireExpiredPurchases(
        jdbcPool: JDBCPool
    ) {
        val query =
            "ALTER TABLE `${getTablePrefix() + tableName}` UPDATE `status` = ?, `statusMessage` = ?, `updatedAt` = ? WHERE `status` = ? AND `expiresAt` < ?;"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    PurchaseStatus.EXPIRED.name,
                    null,
                    System.currentTimeMillis(),
                    PurchaseStatus.PENDING.name,
                    System.currentTimeMillis()
                )
            )
            .await()
    }
}