package co.statu.rule.plugins.payment.db.model

enum class PurchaseStatus {
    EXPIRED,
    FAILED,
    SUCCESS,
    PENDING,
    REFUNDED,
}