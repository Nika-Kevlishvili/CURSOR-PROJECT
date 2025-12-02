package bg.energo.phoenix.model.request.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ObjectionSearchBy;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ObjectionSortingFields;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class BalancingGroupCoordinatorObjectionListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<ChangeOfCbgStatus> changeStatus;

    private List<EntityStatus> entityStatus;

    private List<Long> gridOperatorIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "changeDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate changeDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "changeDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate changeDateTo;

    private ObjectionSearchBy searchBy;

    private Integer numberOfPodsFrom;

    private Integer numberOfPodsTo;

    private ObjectionSortingFields sortBy;

    private Sort.Direction direction;

}
