package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerInvoicesResponseModel {
    Long getInvoiceId();

    String getInvoiceNumber();

    InvoiceDocumentType getDocumentType();

    LocalDate getInvoiceDate();

    String getAccountingPeriod();

    String getBillingNumber();

    String getBasisForIssuing();

    LocalDate getMeterReadingPeriodFrom();

    LocalDate getMeterReadingPeriodTo();

    BigDecimal getTotalAmountExcludingVat();

    InvoiceStatus getStatus();

    String getActions(); // todo ?
}
