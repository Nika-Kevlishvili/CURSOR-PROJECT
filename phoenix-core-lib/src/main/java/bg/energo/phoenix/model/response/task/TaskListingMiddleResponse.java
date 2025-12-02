package bg.energo.phoenix.model.response.task;

import java.time.LocalDateTime;

public interface TaskListingMiddleResponse {

    Long getId();
    String getType();
    String getCurrentPerformer();
    String getPerformer();
    String getStartDate();
    String getEndDate();
    String getCompletionDate();
    String getConnectionType();
    String getStatus();
    String getCurrentStatus();
    LocalDateTime getCreateDate();

}
