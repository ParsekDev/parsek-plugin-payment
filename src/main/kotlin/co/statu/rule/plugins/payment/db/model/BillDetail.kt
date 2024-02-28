package co.statu.rule.plugins.payment.db.model

import co.statu.rule.database.DBEntity
import java.util.*

data class BillDetail(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    var type: BillType = BillType.INDIVIDUAL,
    var addressName: String = "",
    var name: String = "",
    var phone: String = "",
    var province: String = "",
    var district: String = "",
    var address: String = "",
    var taxOrIdNum: String = "",
    var companyName: String = "",
    var taxAdministration: String = "",
    var electronicInvoicePayer: Boolean = false,
    var zipCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : DBEntity()