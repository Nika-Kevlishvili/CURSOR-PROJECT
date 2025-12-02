package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.ApplicationModelType;
import bg.energo.phoenix.model.enums.billing.billings.InvoiceDueDateType;
import bg.energo.phoenix.util.epb.EPBListUtils;

import java.time.LocalDate;
import java.util.List;

public interface BillingRunStandardProcessModel {

    LocalDate getInvoiceDate();

    LocalDate getTaxEventDate();

    InvoiceDueDateType getInvoiceDueDateType();

    LocalDate getInvoiceDueDate();

    String getApplicationModelType();

    Long getAccountPeriodId();

    default List<ApplicationModelType> getApplicationModelEnums() {
        return EPBListUtils.convertDBEnumStringArrayIntoListEnum(ApplicationModelType.class, getApplicationModelType());
    }
}
