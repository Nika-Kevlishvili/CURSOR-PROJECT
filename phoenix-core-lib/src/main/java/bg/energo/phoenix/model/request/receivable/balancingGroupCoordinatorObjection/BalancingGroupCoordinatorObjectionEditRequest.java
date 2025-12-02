package bg.energo.phoenix.model.request.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgCreateStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@BalancingGroupCoordinatorObjectionEditRequestValidator
public class BalancingGroupCoordinatorObjectionEditRequest {
    @NotNull(message = "gridOperatorId-[gridOperatorId] must not be null;")
    private Long gridOperatorId;

    @NotNull(message = "dateOfChange-[dateOfChange] must not be null;")
    @DateRangeValidator(fieldPath = "dateOfChange", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate dateOfChange;

    private Long fileId;

    private List<Long> taskIds;

    private List<@Valid BalancingGroupCoordinatorObjectionProcessResultEditRequest> processEditRequest;

    @NotNull(message = "templateIds-Template ids should be provided!;")
    @NotEmpty(message = "templateIds-Template ids should be provided!;")
    private Set<Long> templateIds;

    @NotNull(message = "emailTemplateId-Email template id can not be null!;")
    private Long emailTemplateId;

    private List<Long> subFileIds = new ArrayList<>();

    private ChangeOfCbgCreateStatus saveAs;

}
