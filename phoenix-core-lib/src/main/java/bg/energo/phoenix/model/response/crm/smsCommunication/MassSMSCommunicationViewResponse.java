package bg.energo.phoenix.model.response.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
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
public class MassSMSCommunicationViewResponse {
    private Long id;
    private List<MassSMSCustomerAndContractResponse> customers;
    private boolean allCustomersWithActiveContract;
    private boolean communicationAsInstitution;
    private CommunicationType communicationType;
    private EntityStatus entityStatus;
    private SmsCommStatus communicationStatus;
    private ShortResponse topicOfCommunication;
    private ShortResponse creatorEmployeeShortResponse;
    private ShortResponse senderEmployeeShortResponse;
    private SmsCommunicationChannel communicationChannel;
    private LocalDateTime dateAndTime;
    private ShortResponse exchangeCode;
    private String messageBody;
    private List<ShortResponse> contactPurposes;
    private List<ShortResponse> relatedCustomers;
    private List<FileWithStatusesResponse> filesShortResponse;
    private List<TaskShortResponse> taskShortResponse;
    private List<SystemActivityShortResponse> activityShortResponse;
    private List<ReportResponse> reports;
    private ContractTemplateShortResponse templateResponse;
}
