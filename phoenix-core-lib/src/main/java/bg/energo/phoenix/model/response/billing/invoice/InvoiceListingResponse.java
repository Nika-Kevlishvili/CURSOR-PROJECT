package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record InvoiceListingResponse(
        Long id,
        String invoiceNumber,
        InvoiceDocumentType invoiceDocumentType,
        LocalDate dateOfInvoice,
        String customer,
        String accountingPeriod,
        String billingRun,
        String basisForIssuing,
        LocalDate meterReadingPeriodFrom,
        LocalDate meterReadingPeriodTo,
        BigDecimal totalAmount,
        InvoiceStatus status
) {
    public InvoiceListingResponse(
            Long id,
            String invoiceNumber,
            InvoiceDocumentType invoiceDocumentType,
            LocalDate dateOfInvoice,
            String customer,
            String accountingPeriod,
            String billingRun,
            String basisForIssuing,
            LocalDate meterReadingPeriodFrom,
            LocalDate meterReadingPeriodTo,
            BigDecimal totalAmount,
            InvoiceStatus status
    ) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDocumentType = invoiceDocumentType;
        this.dateOfInvoice = dateOfInvoice;
        this.customer = customer;
        this.accountingPeriod = accountingPeriod;
        this.billingRun = billingRun;
        this.basisForIssuing = basisForIssuing;
        this.meterReadingPeriodFrom = meterReadingPeriodFrom;
        this.meterReadingPeriodTo = meterReadingPeriodTo;
        this.totalAmount = Objects.requireNonNullElse(totalAmount, BigDecimal.ZERO);
        this.status = status;
    }
}
