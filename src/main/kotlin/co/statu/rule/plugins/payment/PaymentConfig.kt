package co.statu.rule.plugins.payment

import co.statu.parsek.api.config.PluginConfig

data class PaymentConfig(
    val testMode: Boolean = true,
    val expireInSeconds: Int = 60 * 60
) : PluginConfig()