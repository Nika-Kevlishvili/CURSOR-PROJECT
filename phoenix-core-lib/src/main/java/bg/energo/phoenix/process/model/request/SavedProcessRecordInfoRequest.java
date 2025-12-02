package bg.energo.phoenix.process.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedProcessRecordInfoRequest {

    @NotNull(message = "recordId-Record ID must not be null")
    private Long recordId;

    @NotNull(message = "recordIdentifier-Record identifier must not be null")
    private String recordIdentifier;

    @NotNull(message = "success-Record success state must not be null")
    private boolean success;

    private String errorMessage;

    @NotNull(message = "processId-Record process ID must not be null")
    private Long processId;

}
