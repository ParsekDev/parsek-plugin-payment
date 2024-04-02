package co.statu.rule.plugins.payment.route

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.api.config.PluginConfigManager
import co.statu.parsek.error.BadRequest
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.PaymentConfig
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.PaymentSystem
import co.statu.rule.plugins.payment.api.Checkout
import co.statu.rule.plugins.payment.db.dao.BillDetailDao
import co.statu.rule.plugins.payment.db.impl.BillDetailDaoImpl
import co.statu.rule.plugins.payment.error.InvalidBillDetail
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import java.util.*

@Endpoint
class CheckoutAPI(
    private val paymentPlugin: PaymentPlugin
) : LoggedInApi() {
    private val authProvider by lazy {
        paymentPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    private val paymentSystem by lazy {
        paymentPlugin.pluginBeanContext.getBean(PaymentSystem::class.java)
    }

    private val pluginConfigManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PaymentConfig>
    }

    override val paths = listOf(Path("/checkout", RouteType.POST))

    private val billDetailDao: BillDetailDao = BillDetailDaoImpl()

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("type", stringSchema())
                        .requiredProperty("value", stringSchema())
                        .requiredProperty("method", stringSchema())
                        .requiredProperty("billDetailId", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val type = data.getString("type")
        val value = data.getString("value")
        val method = data.getString("method")
        val billDetailId = data.getString("billDetailId")

        validateInput(type, value, method, billDetailId, userId)

        val convertedBillDetailId = UUID.fromString(billDetailId)

        val jdbcPool = databaseManager.getConnectionPool()

        val billDetail = billDetailDao.byId(convertedBillDetailId, jdbcPool)!!

        val typeListener = paymentSystem.getTypeListener(type)!!
        val methodIntegration = paymentSystem.getPaymentMethodIntegration(method)!!
        val user = authProvider.getUser(userId)!!

        val checkout = Checkout(
            user,
            type,
            pluginConfigManager,
            databaseManager,
            billDetail,
            typeListener,
            methodIntegration,
        )

        return typeListener.onHandleCheckout(user, value, checkout)
    }

    private suspend fun validateInput(type: String, value: String, method: String, billDetailId: String, userId: UUID) {
        if (type.isBlank() || value.isBlank() || method.isBlank()) {
            throw BadRequest()
        }

        if (paymentSystem.getTypeListener(type) == null) {
            throw BadRequest()
        }

        val convertedBillDetailId = try {
            UUID.fromString(billDetailId)
        } catch (e: Exception) {
            throw InvalidBillDetail()
        }

        if (paymentSystem.getPaymentMethodIntegration(method) == null) {
            throw BadRequest()
        }

        val jdbcPool = databaseManager.getConnectionPool()

        val exists = billDetailDao.existsByIdAndUserId(convertedBillDetailId, userId, jdbcPool)

        if (!exists) {
            throw InvalidBillDetail()
        }
    }
}