package bg.energo.phoenix.model.request.billing.compensations;

import bg.energo.phoenix.model.enums.billing.compensation.CompensationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

public record CompensationListingRequest(
        String prompt,

        List<CompensationStatus> statuses,

        List<Long> currencies,

        LocalDate date,

        CompensationListingSearchByFields searchBy,

        Integer page,

        @Positive(message = "size-Size must be positive value")
        @NotNull(message = "size-Size must not be null")
        Integer size,

        Sort.Direction direction,

        CompensationListingSortBy sortBy
) {

    @Getter
    @AllArgsConstructor
    public enum CompensationListingSortBy {
        NUMBER("number"),
        DATE("date"),
        PERIOD("period"),
        DOCUMENT_VOLUMES("documentVolumes"),
        PRICE("compensationReason"),
        COMPENSATION_REASON("compensationReason"),
        AMOUNT("amount"),
        CURRENCY("currency"),
        CUSTOMER("customer"),
        POD("pod"),
        RECIPIENT("recipient"),
        STATUS("compensationStatus"),
        INDEX("index"),
        INVOICE_USAGE_DATE("invoice_usage_date"),
        CREATE_DATE("create_date");

        private final String column;
    }

    public enum CompensationListingSearchByFields {
        ALL,
        NUMBER,
        PERIOD,
        DOCUMENT_VOLUMES,
        PRICE,
        COMPENSATION_REASON,
        AMOUNT,
        CUSTOMER,
        POD,
        RECIPIENT,
        INDEX
    }
}
