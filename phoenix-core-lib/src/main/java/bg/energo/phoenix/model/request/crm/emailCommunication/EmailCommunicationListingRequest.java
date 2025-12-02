package bg.energo.phoenix.model.request.crm.emailCommunication;

import bg.energo.phoenix.model.enums.crm.emailCommunication.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailCommunicationListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private EmailCommunicationSearchFields searchBy;

    private List<Long> creatorEmployeeId;

    private List<Long> senderEmployeeId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createDateTo;

    private List<Long> contactPurposeId;

    private List<EmailCommunicationType> communicationType;

    private List<Long> activityId;

    private List<Long> taskId;

    private List<Long> communicationTopicId;

    private List<EmailCommunicationChannelType> kindOfCommunication;

    private List<EmailCommunicationStatus> communicationStatus;

    private Sort.Direction activityDirection;

    private Sort.Direction contactPurposeDirection;

    private EmailCommunicationListColumns sortColumn;

    private Sort.Direction sortDirection;
}
