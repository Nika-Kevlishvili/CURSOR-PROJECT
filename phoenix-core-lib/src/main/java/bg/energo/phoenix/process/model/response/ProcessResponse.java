package bg.energo.phoenix.process.model.response;

import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.process.model.enums.ProcessStatus;
import bg.energo.phoenix.process.model.enums.ProcessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResponse {

    private Long id;

    private ProcessStatus status;

    private ProcessType processType;

    private String name;

    private LocalDateTime creationDate;

    private LocalDateTime startDate;

    private LocalDateTime completeDate;

    //TODO: Implement as part of next iteration
    private String startAfterProcess;

    private LocalDateTime postponedStartDate;

    private String systemUserId;

    private List<ProcessNotificationResponse> processResponses;

    private List<ShortResponse> fileResponse;

    private String massImportFileName;
}
