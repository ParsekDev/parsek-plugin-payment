package co.statu.rule.plugins.payment.db.dao

import co.statu.rule.database.Dao
import co.statu.rule.plugins.payment.db.model.BillDetail
import io.vertx.jdbcclient.JDBCPool
import java.util.*

abstract class BillDetailDao : Dao<BillDetail>(BillDetail::class) {
    abstract suspend fun add(billDetail: BillDetail, jdbcPool: JDBCPool): UUID

    abstract suspend fun existsByIdAndUserId(
        id: UUID,
        userId: UUID,
        jdbcPool: JDBCPool
    ): Boolean

    abstract suspend fun byUserId(
        userId: UUID,
        page: Long,
        jdbcPool: JDBCPool
    ): List<BillDetail>

    abstract suspend fun byIdAndUserId(
        id: UUID,
        userId: UUID,
        jdbcPool: JDBCPool
    ): BillDetail?

    abstract suspend fun countByUserId(
        userId: UUID,
        jdbcPool: JDBCPool
    ): Long

    abstract suspend fun deleteById(
        id: UUID,
        jdbcPool: JDBCPool
    )

    abstract suspend fun update(billDetail: BillDetail, jdbcPool: JDBCPool)
}