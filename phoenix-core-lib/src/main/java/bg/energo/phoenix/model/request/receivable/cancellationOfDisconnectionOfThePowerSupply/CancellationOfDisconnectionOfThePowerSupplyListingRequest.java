package bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public record CancellationOfDisconnectionOfThePowerSupplyListingRequest(
        String prompt,
        @NotNull(message = "page-Page must not be null;")
        Integer page,
        @NotNull(message = "size-Size must not be null;")
        Integer size,
        List<Long> gridOperatorId,
        Integer numberOfPodsFrom,
        Integer numberOfPodsTo,
        List<CancellationOfDisconnectionOfThePowerSupplyStatus> cancellationStatus,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @DateRangeValidator(fieldPath = "createDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31", message = "createDateFrom-You cannot enter date before {fromDate} and after {toDate};")
        LocalDate createDateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @DateRangeValidator(fieldPath = "createDateTo", fromDate = "1990-01-01", toDate = "2090-12-31", message = "createDateTo-You cannot enter date before {fromDate} and after {toDate};")
        LocalDate createDateTo,
        SearchBy searchBy,
        SortBy sortBy,
        Sort.Direction sortDirection,

        List<EntityStatus> statuses
) {
    public enum SearchBy {
        ALL,
        CANCELLATION_NUMBER,
        DISCONNECTION_REQUEST_NUMBER,

        CUSTOMER_IDENTIFIER,

        POD_IDENTIFIER,

        CUSTOMER_NAME
    }

    @Getter
    @AllArgsConstructor
    public enum SortBy {
        DATE("cancellationDate"),
        NUMBER_OF_PODS("numberOfPods"),
        CANCELLATION_STATUS("cancellationStatus"),
        CANCELLATION_NUMBER("id"),
        REQUEST_FOR_DISCONNECTION_THE_POWER_SUPPLY("requestForDisconnectionNumber");

        private final String columnName;
    }
}
