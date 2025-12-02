package bg.energo.phoenix.model.request.crm.emailCommunication;

import bg.energo.phoenix.model.customAnotations.crm.emailCommuncation.EmailCommunicationRequestValidator;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import bg.energo.phoenix.model.enums.crm.massEmailCommunication.EmailCreateType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@EmailCommunicationRequestValidator
public class EmailCommunicationEditRequest {

    private Boolean communicationAsAnInstitution; //Not editable on send

    @Size(max = 128, message = "dmsNumber-[dmsNumber] size should be 128 symbol;")
    private String dmsNumber;

    @NotNull(message = "communicationTopicId-[communicationTopicId] should not be null;")
    private Long communicationTopicId;  //Not editable on send

    @NotNull(message = "emailBoxId-[emailBoxId] should not be null;")
    private Long emailBoxId; //Not editable on send

    @Size(max = 1024, message = "customerEmailAddress-[customerEmailAddress] size should be 1024 symbol;")
    @NotNull(message = "customerEmailAddress-[customerEmailAddress] should not be null;")
    private String customerEmailAddress; //Not editable on send

    @Size(max = 255, message = "emailSubject-[emailSubject] size should be 255 symbol;")
    @NotNull(message = "emailSubject-[emailSubject] should not be null;")
    private String emailSubject; //Not editable on send

    private String emailBody; //Not editable on send

    @NotNull(message = "customerDetailId-[customerDetailId] should not be null;")
    private Long customerDetailId; //Not editable on send

    @NotNull(message = "customerCommunicationId-[customerCommunicationId] should not be null;")
    private Long customerCommunicationId; //Not editable on send

    @NotNull(message = "emailCommunicationType-[emailCommunicationType] should not be null;")
    private EmailCommunicationType emailCommunicationType; //Not editable on send

    @NotNull(message = "createType-[createType] create type is mandatory!;")
    private EmailCreateType emailCreateType;

    private LocalDateTime dateTime;

    private Set<Long> attachmentFileIds;

    private Set<Long> relatedCustomerIds;

    private Set<Long> communicationFileIds;

    private Set<Long> templateIds;

    private Long emailTemplateId;

}
