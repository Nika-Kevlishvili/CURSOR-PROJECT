package bg.energo.phoenix.model.request.billing.invoice;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record InvoiceListingRequest(
        String prompt,
        @NotNull(message = "page-Page must not be null;")
        Integer page,
        @NotNull(message = "size-Size must not be null;")
        Integer size,
        List<InvoiceDocumentType> documentTypes,
        List<Long> accountingPeriodId,
        String billingRun,
        @DateRangeValidator(fieldPath = "dateOfInvoiceFrom", fromDate = "1990-01-01", toDate = "2090-12-31", message = "dateOfInvoiceFrom-You cannot enter date before {fromDate} and after {toDate};")
        LocalDate dateOfInvoiceFrom,
        @DateRangeValidator(fieldPath = "dateOfInvoiceTo", fromDate = "1990-01-01", toDate = "2090-12-31", message = "dateOfInvoiceTo-You cannot enter date before {fromDate} and after {toDate};")
        LocalDate dateOfInvoiceTo,
        @PositiveOrZero(message = "totalAmountFrom-Total Amount From must not be negative;")
        BigDecimal totalAmountFrom,
        @PositiveOrZero(message = "totalAmountTo-Total Amount To must not be negative;")
        BigDecimal totalAmountTo,
        @DateRangeValidator(fieldPath = "meterReadingPeriodFrom", fromDate = "1990-01-01", toDate = "2090-12-31", message = "meterReadingPeriodFrom-You cannot enter date before {fromDate} and after {toDate};")
        LocalDate meterReadingPeriodFrom,
        @DateRangeValidator(fieldPath = "meterReadingPeriodTo", fromDate = "1990-01-01", toDate = "2090-12-31", message = "meterReadingPeriodTo-You cannot enter date before {fromDate} and after {toDate};")
        LocalDate meterReadingPeriodTo,
        List<InvoiceListingStatus> invoiceStatuses,
        InvoiceListingSearchBy searchBy,
        InvoiceListingSortBy sortBy,
        Sort.Direction sortDirection
) {
    public enum InvoiceListingSearchBy {
        ALL,
        ACCOUNTING_PERIOD,
        BILLING_RUN,
        INVOICE_NUMBER
    }

    public enum InvoiceListingStatus {
        CANCELLED, REAL;

        public InvoiceStatus mapToInvoiceStatus() {
            switch (this) {
                case CANCELLED -> {
                    return InvoiceStatus.CANCELLED;
                }
                case REAL -> {
                    return InvoiceStatus.REAL;
                }
                default -> throw new RuntimeException("Invalid listing status");
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum InvoiceListingSortBy {
        @JsonIgnore
        ID("id"),
        INVOICE_NUMBER("invoiceNumber"),
        INVOICE_DOCUMENT_TYPE("invoiceDocumentType"),
        INVOICE_DATE("invoiceDate"),
        CUSTOMER("cd.name"),
        ACCOUNTING_PERIOD("ap.name"),
        BILLING_RUN("bil.billingNumber"),
        BASIS_FOR_ISSUING("basisForIssuing"),
        METER_READING_FROM("meterReadingPeriodFrom"),
        METER_READING_TO("meterReadingPeriodTo"),
        TOTAL_AMOUNT("totalAmountExcludingVat"),
        INVOICE_STATUS("invoiceStatus");

        private final String columnName;
    }

    @JsonIgnore
    @AssertTrue(message = "dateOfInvoiceTo-Date of invoice to cannot be before date of invoice from;")
    public boolean isDateOfInvoicesValid() {
        if (Stream.of(dateOfInvoiceFrom, dateOfInvoiceTo).noneMatch(Objects::isNull)) {
            return !dateOfInvoiceTo.isBefore(dateOfInvoiceFrom);
        }

        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "meterReadingPeriodTo-Meter reading period to cannot be before or equal to meter reading period from;")
    public boolean isMeterReadingPeriodValid() {
        if (Stream.of(meterReadingPeriodFrom, meterReadingPeriodTo).noneMatch(Objects::isNull)) {
            return !meterReadingPeriodTo.isBefore(meterReadingPeriodFrom)
                    && !meterReadingPeriodTo.equals(meterReadingPeriodFrom);
        }

        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "totalAmountTo-Total amount to cannot be less then total amount from;")
    public boolean isTotalAmountValid() {
        if (Stream.of(totalAmountFrom, totalAmountTo).noneMatch(Objects::isNull)) {
            return totalAmountFrom.compareTo(totalAmountTo) < 0;
        }

        return true;
    }
}
