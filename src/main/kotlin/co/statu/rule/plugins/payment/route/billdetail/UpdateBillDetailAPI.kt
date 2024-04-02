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
import co.statu.rule.plugins.payment.db.model.BillType
import co.statu.rule.plugins.payment.util.BillDetailInputValidator
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.Parameters
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaParser
import io.vertx.json.schema.common.dsl.Schemas.*
import java.util.*

@Endpoint
class UpdateBillDetailAPI(
    private val paymentPlugin: PaymentPlugin
) : LoggedInApi() {
    private val authProvider by lazy {
        paymentPlugin.pluginBeanContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        paymentPlugin.pluginBeanContext.getBean(DatabaseManager::class.java)
    }

    override val paths = listOf(Path("/billDetails/:id", RouteType.PUT))

    override fun getValidationHandler(schemaParser: SchemaParser): ValidationHandler =
        ValidationHandlerBuilder.create(schemaParser)
            .pathParameter(Parameters.param("id", stringSchema()))
            .body(
                json(
                    objectSchema()
                        .optionalProperty("type", enumSchema(*BillType.entries.map { it.name }.toTypedArray()))
                        .optionalProperty("addressName", stringSchema())
                        .optionalProperty("name", stringSchema())
                        .optionalProperty("phone", stringSchema())
                        .optionalProperty("province", stringSchema())
                        .optionalProperty("district", stringSchema())
                        .optionalProperty("address", stringSchema())
                        .optionalProperty("taxOrIdNum", stringSchema())
                        .optionalProperty("companyName", stringSchema())
                        .optionalProperty("taxAdministration", stringSchema())
                        .optionalProperty("electronicInvoicePayer", booleanSchema())
                        .optionalProperty("zipCode", stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    private val billDetailDao: BillDetailDao = BillDetailDaoImpl()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val id = parameters.pathParameter("id").string

        UUIDUtil.validate(id)

        val parsedUUID = UUID.fromString(id)

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val type =
            if (data.getString("type") == null)
                null
            else
                BillType.valueOf(data.getString("type"))
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

        val jdbcPool = databaseManager.getConnectionPool()

        val billDetail = billDetailDao.byIdAndUserId(parsedUUID, userId, jdbcPool) ?: throw NotExists()

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
            zipCode,
            billDetail
        )

        var updateBillDetail = false

        if (type != null) {
            billDetail.type = type

            updateBillDetail = true
        }

        if (addressName != null) {
            billDetail.addressName = addressName

            updateBillDetail = true
        }

        if (name != null) {
            billDetail.name = name

            updateBillDetail = true
        }

        if (phone != null) {
            billDetail.phone = phone

            updateBillDetail = true
        }

        if (province != null) {
            billDetail.province = province

            updateBillDetail = true
        }

        if (district != null) {
            billDetail.district = district

            updateBillDetail = true
        }

        if (address != null) {
            billDetail.address = address

            updateBillDetail = true
        }

        if (taxOrIdNum != null) {
            billDetail.taxOrIdNum = taxOrIdNum

            updateBillDetail = true
        }

        if ((type != null && type == BillType.CORPORATE) || (type == null && billDetail.type == BillType.CORPORATE)) {
            if (companyName != null) {
                billDetail.companyName = companyName

                updateBillDetail = true
            }

            if (taxAdministration != null) {
                billDetail.taxAdministration = taxAdministration

                updateBillDetail = true
            }
        }

        if (electronicInvoicePayer != null) {
            billDetail.electronicInvoicePayer = electronicInvoicePayer

            updateBillDetail = true
        }

        if (zipCode != null) {
            billDetail.zipCode = zipCode

            updateBillDetail = true
        }

        if (updateBillDetail) {
            billDetailDao.update(billDetail, jdbcPool)
        }

        return Successful()
    }
}