package co.statu.rule.plugins.payment.util

import co.statu.parsek.model.Error
import co.statu.parsek.model.Errors
import co.statu.rule.plugins.payment.db.model.BillDetail
import co.statu.rule.plugins.payment.db.model.BillType
import co.statu.rule.plugins.payment.error.*

object BillDetailInputValidator {
    suspend fun validate(
        type: BillType?,
        addressName: String?,
        name: String?,
        phone: String?,
        province: String?,
        district: String?,
        address: String?,
        taxOrIdNum: String?,
        companyName: String?,
        taxAdministration: String?,
        zipCode: String?,
        billDetail: BillDetail? = null
    ) {
        val errors = mutableMapOf<String, Error>()

        if (addressName != null && addressName.isBlank()) {
            errors["addressName"] = InvalidAddressName()
        }

        if (name != null && name.isBlank()) {
            errors["name"] = InvalidName()
        }

        if (phone != null && phone.isBlank()) {
            errors["phone"] = InvalidPhone()
        }

        if (province != null && province.isBlank()) {
            errors["province"] = InvalidProvince()
        }

        if (district != null && district.isBlank()) {
            errors["district"] = InvalidDistrict()
        }

        if (address != null && address.isBlank()) {
            errors["address"] = InvalidAddress()
        }

        if (taxOrIdNum != null && taxOrIdNum.isBlank()) {
            errors["taxOrIdNum"] = InvalidTaxOrIdNum()
        }

        if (zipCode != null && zipCode.isBlank()) {
            errors["zipCode"] = InvalidZipCode()
        }

        if ((billDetail == null && type != null && type == BillType.CORPORATE) || (billDetail != null && billDetail.type == BillType.CORPORATE || type != null && type == BillType.CORPORATE)) {
            if (companyName.isNullOrBlank()) {
                errors["companyName"] = InvalidCompanyName()
            }

            if (taxAdministration.isNullOrBlank()) {
                errors["taxAdministration"] = InvalidTaxAdministration()
            }
        }

        if (errors.isNotEmpty()) {
            throw Errors(errors)
        }
    }
}