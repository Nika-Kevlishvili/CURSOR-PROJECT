package bg.energo.phoenix.model.response.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
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
public class SmsCommunicationViewResponse {
    private Long id;
    private boolean communicationAsInstitution;
    private CommunicationType communicationType;
    private String smsBody;
    private LocalDateTime sendDate;
    private SmsCommunicationChannel smsCommunicationChannel;
    private SmsCommStatus communicationStatus;
    private EntityStatus entityStatus;
    private ShortResponse topicOfCommunication;
    private CustomerDetailShortResponse customerShortResponse;
    private ShortResponse creatorEmployeeShortResponse;
    private CustomerCommunicationDataResponse customerCommunicationDataShortResponse;
    private ShortResponse senderEmployeeShortResponse;
    private ShortResponse exchangeCode;
    private String phoneNumber;
    private List<FileWithStatusesResponse> filesShortResponse;
    private List<ShortResponse> relatedCustomersShortResponse;
    private List<TaskShortResponse> taskShortResponse;
    private List<SystemActivityShortResponse> activityShortResponse;
    private ContractTemplateShortResponse templateResponse;
}
