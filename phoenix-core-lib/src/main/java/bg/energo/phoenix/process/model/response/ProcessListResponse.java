package bg.energo.phoenix.process.model.response;

import bg.energo.phoenix.process.model.enums.ProcessStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProcessListResponse {

    private Long id;

    private ProcessStatus status;

    private String name;

    private LocalDateTime creationDate;

    private LocalDateTime startDate;

    private LocalDateTime completeDate;

    //TODO: Implement as part of next iteration
    private String startAfterProcess;

    private LocalDateTime postponedStartDate;

    private String systemUserId;

}
