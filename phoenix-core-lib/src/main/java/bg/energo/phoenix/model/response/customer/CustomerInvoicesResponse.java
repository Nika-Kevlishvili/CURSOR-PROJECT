package bg.energo.phoenix.model.response.customer;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerInvoicesResponse(
        Long invoiceId,
        String invoiceNumber,
        InvoiceDocumentType documentType,
        LocalDate invoiceDate,
        String accountingPeriod,
        String billingRun,
        String basisForIssuing,
        LocalDate meterReadingPeriodFrom,
        LocalDate meterReadingPeriodTo,
        BigDecimal totalAmount,
        InvoiceStatus status,
        String actions
) {
    public CustomerInvoicesResponse(CustomerInvoicesResponseModel model) {
        this(model.getInvoiceId(),
                model.getInvoiceNumber(),
                model.getDocumentType(),
                model.getInvoiceDate(),
                model.getAccountingPeriod(),
                model.getBillingNumber(),
                model.getBasisForIssuing(),
                model.getMeterReadingPeriodFrom(),
                model.getMeterReadingPeriodTo(),
                model.getTotalAmountExcludingVat(),
                model.getStatus(),
                model.getActions());
    }

    public CustomerInvoicesResponse(Long invoiceId,
                                    String invoiceNumber,
                                    InvoiceDocumentType documentType,
                                    LocalDate invoiceDate,
                                    String accountingPeriod,
                                    String billingRun,
                                    String basisForIssuing,
                                    LocalDate meterReadingPeriodFrom,
                                    LocalDate meterReadingPeriodTo,
                                    BigDecimal totalAmount,
                                    InvoiceStatus status,
                                    String actions) {
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.documentType = documentType;
        this.invoiceDate = invoiceDate;
        this.accountingPeriod = accountingPeriod;
        this.billingRun = billingRun;
        this.basisForIssuing = basisForIssuing;
        this.meterReadingPeriodFrom = meterReadingPeriodFrom;
        this.meterReadingPeriodTo = meterReadingPeriodTo;
        this.totalAmount = totalAmount;
        this.status = status;
        this.actions = actions;
    }
}
