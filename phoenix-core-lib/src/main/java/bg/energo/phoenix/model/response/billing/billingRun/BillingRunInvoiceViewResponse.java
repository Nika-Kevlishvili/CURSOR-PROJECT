package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BillingRunInvoiceViewResponse {
    Long getId();
    String getInvoiceNumber();
    InvoiceDocumentType getInvoiceDocumentType();
    InvoiceType getInvoiceType();
    LocalDate getInvoiceDate();
    String getCustomer();
    String getAccountingPeriod();
    String getBillingRun();
    String getBasisForIssuing();
    LocalDate getMeterReadingPeriodFrom();
    LocalDate getMeterReadingPeriodTo();
    BigDecimal getTotalAmount();

    Boolean getIsMarkedAsRemoved();
}
