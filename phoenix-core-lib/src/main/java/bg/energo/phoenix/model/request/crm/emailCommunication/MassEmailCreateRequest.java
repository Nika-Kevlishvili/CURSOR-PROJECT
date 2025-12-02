package bg.energo.phoenix.model.request.crm.emailCommunication;

import bg.energo.phoenix.model.customAnotations.crm.emailCommuncation.MassEmailCommunicationRequestValidator;
import bg.energo.phoenix.model.enums.crm.massEmailCommunication.EmailCreateType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@MassEmailCommunicationRequestValidator
public class MassEmailCreateRequest {

    private boolean allCustomersWithActiveContract;

    private @Valid Set<MassCommunicationCustomerRequest> customers;

    private Boolean communicationAsInstitution;

    @NotNull(message = "topicOfCommunicationId-[topicOfCommunicationId] topic of communication is mandatory!;")
    private Long topicOfCommunicationId;

    @NotNull(message = "emailBoxId-[emailBoxId] emailBoxId is mandatory!;")
    private Long emailBoxId;

    @NotBlank(message = "subject-[subject] email subject is mandatory!;")
    @Size(min = 1, max = 255, message = "subject-subject of Customers should be between {min} and {max} characters.;")
    private String subject;

    private String emailBody;

    @NotNull(message = "contactPurposeId-[contactPurposeId] contact purpose is mandatory!;")
    private Set<Long> contactPurposeIds;

    private Set<Long> relatedCustomerIds;

    private Set<Long> communicationFileIds;

    private Set<Long> attachmentFileIds;

    @NotNull(message = "createType-[createType] create type is mandatory!;")
    private EmailCreateType createType;

    private Long emailTemplateId;

    private Set<Long> templateIds;
}
