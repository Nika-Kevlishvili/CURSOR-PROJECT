package bg.energo.phoenix.model.request.billing.accountingPeriod;

import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodSearchByEnums;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AccountingPeriodsListingRequest {
    private String prompt;

    private AccountingPeriodListColumns sortBy;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private AccountingPeriodStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate closedOnDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate closedOnDateTo;

    private AccountingPeriodSearchByEnums searchBy;

    private Sort.Direction direction;
}
