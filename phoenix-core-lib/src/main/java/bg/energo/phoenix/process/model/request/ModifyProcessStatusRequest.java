package bg.energo.phoenix.process.model.request;

import bg.energo.phoenix.process.model.enums.ProcessStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifyProcessStatusRequest {

    @NotNull(message = "processStatus-Process status must not be null; ")
    private ProcessStatus processStatus;

    @NotNull(message = "processId-Process id must not be null; ")
    private Long processId;

}
