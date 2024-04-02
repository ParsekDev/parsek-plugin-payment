package co.statu.rule.plugins.payment.route.billdetail

import co.statu.parsek.annotation.Endpoint
import co.statu.parsek.model.Path
import co.statu.parsek.model.Result
import co.statu.parsek.model.RouteType
import co.statu.parsek.model.Successful
import co.statu.rule.auth.api.LoggedInApi
import co.statu.rule.auth.provider.AuthProvider
import co.statu.rule.database.DatabaseManager
import co.statu.rule.plugins.payment.PaymentPlugin
import co.statu.rule.plugins.payment.db.dao.BillDetailDao
import co.statu.rule.plugins.payment.db.impl.BillDetailDaoImpl
import co.statu.rule.plugins.payment.db.model.BillDetail
import co.statu.rule.plugins.payment.db.model.BillType
import co.statu.rule.plugins.payment.error.TooManyBillDetails
import co.statu.rule.plugins.payment.util.BillDetailInputValidator
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class AddBillDetailAPI(
    private val paymentPlugin: PaymentPlugin
) : LoggedInApi() {
    private val authProvider by lazy {
        paymentPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    override val paths = listOf(Path("/billDetails", RouteType.POST))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("type", enumSchema(*BillType.entries.map { it.name }.toTypedArray()))
                        .requiredProperty("addressName", stringSchema())
                        .requiredProperty("name", stringSchema())
                        .requiredProperty("phone", stringSchema())
                        .requiredProperty("province", stringSchema())
                        .requiredProperty("district", stringSchema())
                        .requiredProperty("address", stringSchema())
                        .requiredProperty("taxOrIdNum", stringSchema())
                        .optionalProperty("companyName", stringSchema())
                        .optionalProperty("taxAdministration", stringSchema())
                        .requiredProperty("electronicInvoicePayer", booleanSchema())
                        .requiredProperty("zipCode", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    private val billDetailDao: BillDetailDao = BillDetailDaoImpl()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val type = BillType.valueOf(data.getString("type"))
        val addressName = data.getString("addressName")
        val name = data.getString("name")
        val phone = data.getString("phone")
        val province = data.getString("province")
        val district = data.getString("district")
        val address = data.getString("address")
        val taxOrIdNum = data.getString("taxOrIdNum")
        val companyName = data.getString("companyName")
        val taxAdministration = data.getString("taxAdministration")
        val electronicInvoicePayer = data.getBoolean("electronicInvoicePayer")
        val zipCode = data.getString("zipCode")

        BillDetailInputValidator.validate(
            type,
            addressName,
            name,
            phone,
            province,
            district,
            address,
            taxOrIdNum,
            companyName,
            taxAdministration,
            zipCode
        )

        val billDetail = BillDetail(
            userId = userId,
            type = type,
            addressName = addressName,
            name = name,
            phone = phone,
            province = province,
            district = district,
            address = address,
            taxOrIdNum = taxOrIdNum,
            companyName = companyName,
            taxAdministration = taxAdministration,
            electronicInvoicePayer = electronicInvoicePayer,
            zipCode = zipCode
        )

        val jdbcPool = databaseManager.getConnectionPool()

        val count = billDetailDao.countByUserId(userId, jdbcPool)

        if (count == 10L) {
            throw TooManyBillDetails()
        }

        billDetailDao.add(billDetail, jdbcPool)

        return Successful(mapOf("id" to billDetail.id))
    }
}