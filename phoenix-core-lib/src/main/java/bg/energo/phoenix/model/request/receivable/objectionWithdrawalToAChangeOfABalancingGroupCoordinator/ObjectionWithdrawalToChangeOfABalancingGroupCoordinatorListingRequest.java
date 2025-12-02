package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class ObjectionWithdrawalToChangeOfABalancingGroupCoordinatorListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    List<ObjectionWithdrawalToChangeOfCbgStatus> withdrawalToChangeOfCbgStatuses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateTo;

    private Long numberOfPodsFrom;

    private Long numberOfPodsTo;

    private ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListColumns columns;

    private ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorSearchFields searchFields;

    private Sort.Direction direction;
}
