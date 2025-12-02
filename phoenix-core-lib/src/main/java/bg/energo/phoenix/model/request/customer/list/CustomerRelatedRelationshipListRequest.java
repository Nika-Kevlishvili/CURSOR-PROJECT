package bg.energo.phoenix.model.request.customer.list;

import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.enums.customer.list.customerRelatedRelationship.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRelatedRelationshipListRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private List<CustomerRelatedRelationshipType> relationshipType;

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

    private List<CustomerRelationshipKindOfCommunication> kindOfCommunications;

    private List<CustomerRelationshipStatuses> communicationStatuses;

    private CustomerRelatedRelationshipSearchField searchBy;

    private CustomerRelatedRelationshipSortField sortBy;

    private Sort.Direction activityDirection;

    private Sort.Direction contactPurposeDirection;

    private Sort.Direction sortingDirection;

}
