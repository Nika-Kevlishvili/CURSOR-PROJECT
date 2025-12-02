package bg.energo.phoenix.model.request.crm.smsCommunication;

import bg.energo.phoenix.model.enums.crm.smsCommunication.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SmsCommunicationListingRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<Long> creatorEmployee;

    private List<Long> senderEmployee;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;

    private List<Long> contactPurposes;

    private List<CommunicationType> communicationTypes;

    private List<Long> activities;

    private List<Long> tasks;

    private List<Long> topicOfCommunications;

    private List<KindOfCommunication> kindOfCommunications;

    private List<SmsCommStatus> communicationStatuses;

    private SmsCommunicationSearchBy smsCommunicationSearchBy;

    private SmsCommunicationListingSortBy sortBy;

    private Sort.Direction activityDirection;

    private Sort.Direction contactPurposeDirection;

    private Sort.Direction sortingDirection;

}
