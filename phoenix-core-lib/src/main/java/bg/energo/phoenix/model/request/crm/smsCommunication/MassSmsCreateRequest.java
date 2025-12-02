package bg.energo.phoenix.model.request.crm.smsCommunication;

import bg.energo.phoenix.model.customAnotations.crm.smsCommunication.MassSmsCommunicationCreateRequestValidator;
import bg.energo.phoenix.model.enums.crm.massSmsCommunication.MassSMSSaveAs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@MassSmsCommunicationCreateRequestValidator
@Builder
@AllArgsConstructor
public class MassSmsCreateRequest {

    private boolean allCustomersWithActiveContract;

    private @Valid Set<MassSMSCustomerRequest> customers;

    private boolean communicationAsInstitution;

    @NotNull(message = "topicOfCommunicationId-[topicOfCommunicationId] topic of communication is mandatory!;")
    private Long topicOfCommunicationId;

    @NotNull(message = "exchangeCodeId-[exchangeCodeId] exchangeCodeId is mandatory!;")
    private Long exchangeCodeId;

    private String smsBody;

    @NotNull(message = "contactPurposeId-[contactPurposeId] contact purpose is mandatory!;")
    private Set<Long> contactPurposeIds;

    private Set<Long> relatedCustomerIds;

    private Set<Long> communicationFileIds;

    @NotNull(message = "saveAs-[saveAs] save as is mandatory!;")
    private MassSMSSaveAs saveAs;

    private Long templateId;

}
