package co.statu.rule.plugins.payment.db.model

import co.statu.rule.database.DBEntity
import io.vertx.core.json.JsonObject
import java.util.*

data class Purchase(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val amount: Long,
    val type: String,
    val price: Long,
    val claims: JsonObject = JsonObject(),
    val status: PurchaseStatus,
    val statusMessage: String? = null,
    val billDetail: JsonObject,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
) : DBEntity()