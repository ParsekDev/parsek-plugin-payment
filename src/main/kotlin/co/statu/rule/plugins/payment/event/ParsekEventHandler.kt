package co.statu.rule.plugins.payment.event

import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.api.event.ParsekEventListener
import co.statu.parsek.config.ConfigManager
import co.statu.rule.plugins.payment.PaymentConfig
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.PaymentPlugin.Companion.logger

class ParsekEventHandler : ParsekEventListener {
    override suspend fun onConfigManagerReady(configManager: ConfigManager) {
        PaymentPlugin.pluginConfigManager = PluginConfigManager(
            configManager,
            PaymentPlugin.INSTANCE,
            PaymentConfig::class.java,
            logger,
            listOf()
        )

        logger.info("Initialized plugin config")
    }
}