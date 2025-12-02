package bg.energo.phoenix.model.request.customer;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record CustomerInvoicesRequest(
        String prompt,
        @NotNull(message = "page-Page must not be null;")
        Integer page,
        @NotNull(message = "size-Size must not be null;")
        Integer size,
        List<InvoiceDocumentType> documentTypes,
        List<Long> accountingPeriodIds,
        String billingRun,
        @DateRangeValidator(fieldPath = "dateOfInvoiceFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOfInvoiceFrom,
        @DateRangeValidator(fieldPath = "dateOfInvoiceFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOfInvoiceTo,
        BigDecimal totalAmountFrom,
        BigDecimal totalAmountTo,
        List<InvoiceStatus> invoiceStatuses,
        SearchBy searchBy,
        SortBy sortBy,
        Sort.Direction direction
) {
    @Getter
    @AllArgsConstructor
    public enum SortBy {
        ID("invoiceId"),
        INVOICE_NUMBER("invoiceNumber"),
        DOCUMENT_TYPE("documentType"),
        INVOICE_DATE("invoiceDate"),
        ACCOUNTING_PERIOD("accountingPeriod"),
        BILLING_RUN("billingNumber"),
        BASIS_FOR_ISSUING("basisForIssuing"),
        METER_READING_FROM("meterReadingPeriodFrom"),
        METER_READING_TO("meterReadingPeriodTo"),
        TOTAL_AMOUNT("totalAmountExcludingVat"),
        STATUS("status"),
        ACTION("actions");

        private final String columnName;
    }

    enum InvoiceDocumentType {
        INVOICE,
        CREDIT_NOTE,
        DEBIT_NOTE,
    }

    enum InvoiceStatus {
        CANCELLED,
        REAL
    }

    public enum SearchBy {
        ACCOUNTING_PERIOD,
        BILLING_RUN,
        INVOICE_NUMBER
    }

    @JsonIgnore
    @AssertTrue(message = "dateOfInvoiceFrom-Date from must be less then date to;")
    public boolean isDateOfInvoiceFromValid() {
        if (Stream.of(dateOfInvoiceFrom, dateOfInvoiceTo).noneMatch(Objects::isNull)) {
            return !this.dateOfInvoiceFrom.isAfter(dateOfInvoiceTo);
        }
        return true;
    }
}
