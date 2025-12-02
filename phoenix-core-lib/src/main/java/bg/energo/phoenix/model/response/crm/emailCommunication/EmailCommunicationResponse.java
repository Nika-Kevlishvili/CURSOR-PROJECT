package bg.energo.phoenix.model.response.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationChannelType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailCommunicationResponse {

    private Long id;
    private Boolean communicationAsAnInstitution;
    private EmailCommunicationChannelType communicationChannelType;
    private String dmsNumber;
    private EmailCommunicationType communicationType;
    private EmailCommunicationStatus emailCommunicationStatus;
    private LocalDateTime sentDate;
    private String emailSubject;
    private String emailBody;
    private String customerEmailAddress;
    private EntityStatus entityStatus;
    private ShortResponse topicOfCommunicationShortResponse;
    private ShortResponse emailBoxShortResponse;
    private EmailConnectedCustomerShortResponse customerShortResponse;
    private ShortResponse creatorEmployeeShortResponse;
    private ShortResponse senderEmployeeShortResponse;
    private EmailConnectedCustomerCommunicationDataShortResponse customerCommunicationDataShortResponse;
    private List<FileWithStatusesResponse> filesShortResponse;
    private List<AttachmentShortResponse> attachmentsShortResponse;
    private List<ShortResponse> relatedCustomersShortResponse;
    private List<TaskShortResponse> taskShortResponse;
    private List<SystemActivityShortResponse> activityShortResponse;
    private Boolean isResendActive;
    private List<ContractTemplateShortResponse> templateResponses;
    private ContractTemplateShortResponse emailTemplateResponse;
    private CreationType emailCreationType;

}
