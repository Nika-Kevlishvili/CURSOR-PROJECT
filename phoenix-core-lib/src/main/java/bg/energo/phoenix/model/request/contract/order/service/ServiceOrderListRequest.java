package bg.energo.phoenix.model.request.contract.order.service;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderSearchField;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderTableColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Data
@Builder
@PromptSymbolReplacer
public class ServiceOrderListRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<Long> serviceDetailIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfCreationFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfCreationTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceMaturityDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceMaturityDateTo;

    private Set<Boolean> invoicePaid;

    private Boolean directDebit;

    private List<Long> accountManagers;

    private ServiceOrderSearchField searchBy;

    private ServiceOrderTableColumn sortBy;

    private Sort.Direction sortDirection;

    @JsonIgnore
    @AssertTrue(message = "invoiceMaturityDateFrom-Invoice maturity date to cannot be before invoice maturity date from;")
    public boolean isInvoiceMaturityDatesValid() {
        if (Stream.of(invoiceMaturityDateFrom, invoiceMaturityDateTo).noneMatch(Objects::isNull)) {
            return !invoiceMaturityDateTo.isBefore(invoiceMaturityDateFrom);
        }

        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "dateOfCreationFrom-Date of order creation to cannot be before date of order creation from;")
    public boolean isDatesOfOrderCreationValid() {
        if (Stream.of(dateOfCreationFrom, dateOfCreationTo).noneMatch(Objects::isNull)) {
            return !dateOfCreationTo.isBefore(dateOfCreationFrom);
        }

        return true;
    }

}
