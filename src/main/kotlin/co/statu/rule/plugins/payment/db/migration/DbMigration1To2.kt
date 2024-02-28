package co.statu.rule.plugins.payment.db.migration

import co.statu.rule.database.DatabaseMigration
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.coroutines.await

class DbMigration1To2(
    override val FROM_SCHEME_VERSION: Int = 1,
    override val SCHEME_VERSION: Int = 2,
    override val SCHEME_VERSION_INFO: String = "Rename isEinvoicePayer to electronicInvoicePayer in bill detail table"
) : DatabaseMigration() {
    override val handlers: List<suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit> = listOf(
        renameIsEinvoicePayerToEInvoicePayer()
    )

    private fun renameIsEinvoicePayerToEInvoicePayer(): suspend (jdbcPool: JDBCPool, tablePrefix: String) -> Unit =
        { jdbcPool: JDBCPool, tablePrefix: String ->
            jdbcPool
                .query("ALTER TABLE `${tablePrefix}bill_detail` RENAME COLUMN `isEinvoicePayer` to `electronicInvoicePayer`;")
                .execute()
                .await()
        }
}