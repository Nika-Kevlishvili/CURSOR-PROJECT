package bg.energo.phoenix.model.request.customer.communicationData;

import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.EditCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.EditContactPersonRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.EditContactPurposeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditCustomerCommunicationsRequest extends BaseCustomerCommunicationsRequest {

    private Long id;

    @Valid
    @Size(min = 1, message = "communicationData.contactPurposes-Customer communications should have at least {min} contact purpose;")
    private List<EditContactPurposeRequest> contactPurposes;

    @Valid
    private List<EditContactPersonRequest> contactPersons;

    @Valid
    private List<EditCommunicationContactRequest> communicationContacts;

}
