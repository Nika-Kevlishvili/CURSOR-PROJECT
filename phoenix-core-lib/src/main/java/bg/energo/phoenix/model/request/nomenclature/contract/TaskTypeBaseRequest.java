package bg.energo.phoenix.model.request.nomenclature.contract;

import bg.energo.phoenix.model.customAnotations.contract.TaskTypeStagesValidator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskTypeBaseRequest {
    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 512, message = "name-Name has invalid length: [{min}:{max}]")
    private String name;

    @NotNull(message = "calendarId-Calendar ID must not be null;")
    private Long calendarId;

    @TaskTypeStagesValidator
    private List<TaskTypeStageRequest> taskTypeStages;

    @NotNull(message = "status-Status must not be null;")
    private NomenclatureItemStatus status;

    @NotNull(message = "defaultSelection-Default selection must not be null;")
    private Boolean defaultSelection;
}
