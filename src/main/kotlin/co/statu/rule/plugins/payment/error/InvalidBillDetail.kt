package co.statu.rule.plugins.payment.error

import co.statu.parsek.model.Error

class InvalidBillDetail(
    statusMessage: String = "",
    extras: Map<String, Any> = mapOf()
) : Error(422, statusMessage, extras)