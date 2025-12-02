package phoenix.core.customer.model.request.communicationData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import phoenix.core.customer.model.request.communicationData.communicationContact.EditCommunicationContactRequest;
import phoenix.core.customer.model.request.communicationData.contactPerson.EditContactPersonRequest;
import phoenix.core.customer.model.request.communicationData.contactPurpose.EditContactPurposeRequest;

import javax.validation.Valid;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditCommunicationDataRequest extends BaseCommunicationDataRequest {

    private Long id;

    @Valid
    private List<EditContactPurposeRequest> contactPurposes;

    @Valid
    private List<EditContactPersonRequest> contactPersons;

    @Valid
    private List<EditCommunicationContactRequest> communicationContacts;

}
