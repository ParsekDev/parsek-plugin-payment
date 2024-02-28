package co.statu.rule.plugins.payment.db.impl

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.plugins.payment.db.dao.BillDetailDao
import co.statu.rule.plugins.payment.db.model.BillDetail
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.*

class BillDetailDaoImpl : BillDetailDao() {

    override suspend fun init(jdbcPool: JDBCPool, plugin: ParsekPlugin) {
        jdbcPool
            .query(
                """
                        CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                            `id` UUID NOT NULL,
                            `userId` UUID NOT NULL,
                            `type` String NOT NULL,
                            `addressName` String NOT NULL,
                            `name` String NOT NULL,
                            `phone` String NOT NULL,
                            `province` String NOT NULL,
                            `district` String NOT NULL,
                            `address` String NOT NULL,
                            `taxOrIdNum` String NOT NULL,
                            `companyName` String NOT NULL,
                            `taxAdministration` String NOT NULL,
                            `electronicInvoicePayer` Boolean DEFAULT false,
                            `zipCode` String NOT NULL,
                            `createdAt` Int64 NOT NULL,
                            `updatedAt` Int64 NOT NULL
                        ) ENGINE = MergeTree() order by `createdAt`;
                        """
            )
            .execute()
            .await()
    }

    override suspend fun add(billDetail: BillDetail, jdbcPool: JDBCPool): UUID {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (${fields.toTableQuery()}) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    billDetail.id,
                    billDetail.userId,
                    billDetail.type,
                    billDetail.addressName,
                    billDetail.name,
                    billDetail.phone,
                    billDetail.province,
                    billDetail.district,
                    billDetail.address,
                    billDetail.taxOrIdNum,
                    billDetail.companyName,
                    billDetail.taxAdministration,
                    billDetail.electronicInvoicePayer,
                    billDetail.zipCode,
                    billDetail.createdAt,
                    billDetail.updatedAt
                )
            )
            .await()

        return billDetail.id
    }

    override suspend fun existsByIdAndUserId(
        id: UUID,
        userId: UUID,
        jdbcPool: JDBCPool
    ): Boolean {
        val query =
            "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` where `id` = ? and `userId` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    id,
                    userId
                )
            )
            .await()

        return rows.toList()[0].getLong(0) == 1L
    }

    override suspend fun byUserId(
        userId: UUID,
        page: Long,
        jdbcPool: JDBCPool
    ): List<BillDetail> {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `userId` = ? LIMIT 10 ${if (page == 1L) "" else "OFFSET ${(page - 1) * 10}"}"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId))
            .await()

        return rows.toEntities()
    }

    override suspend fun byIdAndUserId(
        id: UUID,
        userId: UUID,
        jdbcPool: JDBCPool
    ): BillDetail? {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `id` = ? AND `userId` = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(id, userId))
            .await()

        if (rows.size() == 0) {
            return null
        }

        val row = rows.toList()[0]

        return row.toEntity()
    }

    override suspend fun countByUserId(
        userId: UUID,
        jdbcPool: JDBCPool
    ): Long {
        val query =
            "SELECT COUNT(*) FROM `${getTablePrefix() + tableName}` WHERE userId = ?"

        val rows: RowSet<Row> = jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(userId))
            .await()

        return rows.toList()[0].getLong(0)
    }

    override suspend fun deleteById(
        id: UUID,
        jdbcPool: JDBCPool
    ) {
        val query =
            "DELETE FROM `${getTablePrefix() + tableName}` WHERE `id` = ?"

        jdbcPool
            .preparedQuery(query)
            .execute(Tuple.of(id))
            .await()
    }

    override suspend fun update(billDetail: BillDetail, jdbcPool: JDBCPool) {
        val query =
            "UPDATE `${getTablePrefix() + tableName}` SET `type` = ?, `addressName` = ?, `name` = ?, `phone` = ?, `province` = ?, `district` = ?, `address` = ?, `taxOrIdNum` = ?, `companyName` = ?, `taxAdministration` = ?, `electronicInvoicePayer` = ?, `zipCode` = ?, `updatedAt` = ? WHERE `id` = ?"

        val parameters = Tuple.tuple()

        parameters.addString(billDetail.type.name)
        parameters.addString(billDetail.addressName)
        parameters.addString(billDetail.name)
        parameters.addString(billDetail.phone)
        parameters.addString(billDetail.province)
        parameters.addString(billDetail.district)
        parameters.addString(billDetail.address)
        parameters.addString(billDetail.taxOrIdNum)
        parameters.addString(billDetail.companyName)
        parameters.addString(billDetail.taxAdministration)
        parameters.addBoolean(billDetail.electronicInvoicePayer)
        parameters.addString(billDetail.zipCode)
        parameters.addLong(System.currentTimeMillis())

        parameters.addUUID(billDetail.id)

        jdbcPool
            .preparedQuery(query)
            .execute(parameters)
            .await()
    }
}