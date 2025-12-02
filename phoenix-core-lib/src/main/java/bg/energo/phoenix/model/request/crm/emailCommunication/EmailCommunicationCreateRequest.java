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
public class EmailCommunicationCreateRequest {

    private Boolean communicationAsAnInstitution;

    @Size(max = 128, message = "dmsNumber-[dmsNumber] size should be max 128 symbol;")
    private String dmsNumber;

    @NotNull(message = "communicationTopicId-[communicationTopicId] should not be null;")
    private Long communicationTopicId;

    @NotNull(message = "emailCommunicationType-[emailCommunicationType] should not be null;")
    private EmailCommunicationType emailCommunicationType;

    @NotNull(message = "emailCreateType-[emailCreateType] should not be null;")
    private EmailCreateType emailCreateType;

    private LocalDateTime dateTime;

    @NotNull(message = "emailBoxId-[emailBoxId] should not be null;")
    private Long emailBoxId;

    @Size(max = 1024, message = "customerEmailAddress-[customerEmailAddress] size should be 1024 symbol;")
    @NotNull(message = "customerEmailAddress-[customerEmailAddress] should not be null;")
    private String customerEmailAddress;

    @Size(max = 255, message = "emailSubject-[emailSubject] size should be 255 symbol;")
    @NotNull(message = "emailSubject-[emailSubject] should not be null;")
    private String emailSubject;

    private String emailBody;

    @NotNull(message = "customerDetailId-[customerDetailId] should not be null;")
    private Long customerDetailId;

    @NotNull(message = "customerCommunicationId-[customerCommunicationId] should not be null;")
    private Long customerCommunicationId;

    private Set<Long> communicationFileIds;

    private Set<Long> attachmentFileIds;

    private Set<Long> relatedCustomerIds;

    private Set<Long> templateIds;

    private Long emailTemplateId;
}
