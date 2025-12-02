package bg.energo.phoenix.model.response.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationChannelType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MassEmailCommunicationResponse {

    private Long id;
    private boolean communicationAsInstitution;
    private EmailCommunicationType communicationType;
    private String emailBody;
    private String emailSubject;
    private LocalDateTime sendDate;
    private EmailCommunicationChannelType communicationChannelType;
    private EmailCommunicationStatus emailCommunicationStatus;
    private EntityStatus entityStatus;
    private ShortResponse emailBoxShortResponse;
    private ShortResponse topicOfCommunication;
    private ShortResponse creatorEmployeeShortResponse;
    private ShortResponse senderEmployeeShortResponse;
    private ShortResponse reportFileShortResponse;
    private List<ShortResponse> customersShortResponse;
    private List<ShortResponse> contactPurposesShortResponse;
    private List<FileWithStatusesResponse> filesShortResponse;
    private List<AttachmentShortResponse> attachmentFilesShortResponse;
    private List<ShortResponse> relatedCustomersShortResponse;
    private List<TaskShortResponse> taskShortResponse;
    private List<SystemActivityShortResponse> activityShortResponse;
    private ContractTemplateShortResponse emailTemplateResponse;
    private List<ContractTemplateShortResponse> templateResponses;

}
