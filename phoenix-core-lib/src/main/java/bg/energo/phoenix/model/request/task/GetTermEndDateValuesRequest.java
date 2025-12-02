package bg.energo.phoenix.model.request.task;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetTermEndDateValuesRequest {
    @NotNull(message = "taskTypeId-Task Type must not be null;")
    private Long taskTypeId;

    @NotEmpty(message = "taskStageDetailRequestList-Task stages must not be empty;")
    private List<TaskStageDetailRequest> taskStageDetailRequestList;

}
