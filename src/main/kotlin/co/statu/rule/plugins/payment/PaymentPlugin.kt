package co.statu.rule.plugins.payment

import co.statu.parsek.api.ParsekPlugin
import co.statu.rule.database.Dao
import co.statu.rule.database.DatabaseMigration
import co.statu.rule.database.api.DatabaseHelper
import co.statu.rule.plugins.payment.db.impl.BillDetailDaoImpl
import co.statu.rule.plugins.payment.db.impl.PurchaseDaoImpl
import co.statu.rule.plugins.payment.db.migration.DbMigration1To2

class PaymentPlugin : ParsekPlugin(), DatabaseHelper {
    override val tables: List<Dao<*>> by lazy {
        mutableListOf<Dao<*>>(
            PurchaseDaoImpl(),
            BillDetailDaoImpl()
        )
    }

    override val migrations: List<DatabaseMigration> by lazy {
        listOf<DatabaseMigration>(
            DbMigration1To2()
        )
    }
}