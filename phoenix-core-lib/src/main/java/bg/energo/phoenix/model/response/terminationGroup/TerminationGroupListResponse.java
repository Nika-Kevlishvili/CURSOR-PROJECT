package bg.energo.phoenix.model.response.terminationGroup;

import java.time.LocalDateTime;

public interface TerminationGroupListResponse {

    Long getGroupId();

    String getName();

    Long getNumberOfTerminations();

    LocalDateTime getDateOfCreation();

    String getStatus();

}
