package co.statu.rule.plugins.payment.event

import co.statu.parsek.api.annotation.EventListener
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.api.event.CoreEventListener
import co.statu.parsek.config.ConfigManager
import co.statu.rule.plugins.payment.PaymentConfig
import co.statu.rule.plugins.payment.PaymentPlugin
import org.slf4j.Logger

@EventListener
class CoreEventHandler(
    private val paymentPlugin: PaymentPlugin,
    private val logger: Logger
) : CoreEventListener {
    override suspend fun onConfigManagerReady(configManager: ConfigManager) {
        val pluginConfigManager = PluginConfigManager(
            configManager,
            paymentPlugin,
            PaymentConfig::class.java,
            listOf(),
            listOf("payment")
        )

        paymentPlugin.pluginBeanContext.beanFactory.registerSingleton(
            pluginConfigManager.javaClass.name,
            pluginConfigManager
        )

        logger.info("Initialized plugin config")
    }
}