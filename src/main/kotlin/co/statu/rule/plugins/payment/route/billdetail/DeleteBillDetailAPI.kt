package co.statu.rule.plugins.payment.route.billdetail

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.error.NotExists
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.parsek.util.UUIDUtil
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.db.dao.BillDetailDao
import co.statu.rule.plugins.payment.db.impl.BillDetailDaoImpl
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas
import java.util.*

@Endpoint
class DeleteBillDetailAPI(
    private val paymentPlugin: PaymentPlugin
) : LoggedInApi() {
    private val authProvider by lazy {
        paymentPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    override val paths = listOf(Path("/billDetails/:id", RouteType.DELETE))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .pathParameter(Parameters.param("id", Schemas.stringSchema()))
            .build()

    private val billDetailDao: BillDetailDao = BillDetailDaoImpl()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)

        val id = parameters.pathParameter("id").string

        UUIDUtil.validate(id)

        val parsedUUID = UUID.fromString(id)

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val jdbcPool = databaseManager.getConnectionPool()

        val exists = billDetailDao.existsByIdAndUserId(parsedUUID, userId, jdbcPool)

        if (!exists) {
            throw NotExists()
        }

        billDetailDao.deleteById(parsedUUID, jdbcPool)

        return Successful()
    }
}