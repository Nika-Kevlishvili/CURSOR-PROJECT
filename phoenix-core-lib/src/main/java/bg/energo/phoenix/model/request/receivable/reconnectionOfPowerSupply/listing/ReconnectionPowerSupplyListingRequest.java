package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.listing;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReconnectionPowerSupplyListingRequest {

    @Size(min = 1, message = "prompt-Prompt length must be 1 or more")
    private String prompt;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private Sort.Direction psdrDirection;

    private ReconnectionPowerSupplyListingListColumns sortBy;

    private ReconnectionPowerSupplyListingSearchByEnums searchBy;

    private Sort.Direction direction;

    @DateRangeValidator(fieldPath = "createDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createDateFrom;

    @DateRangeValidator(fieldPath = "createDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createDateTo;

    private List<ReconnectionStatus> reconnectionStatus;

    private List<EntityStatus> statuses;

    private List<Long> gridOperatorIds;

    private Long numberOfPodsFrom;

    private Long numberOfPodsTo;

}
