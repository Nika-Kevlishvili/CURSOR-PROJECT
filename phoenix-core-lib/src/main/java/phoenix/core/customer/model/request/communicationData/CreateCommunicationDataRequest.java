package phoenix.core.customer.model.request.communicationData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import phoenix.core.customer.model.request.communicationData.communicationContact.CreateCommunicationContactRequest;
import phoenix.core.customer.model.request.communicationData.contactPerson.CreateContactPersonRequest;
import phoenix.core.customer.model.request.communicationData.contactPurpose.CreateContactPurposeRequest;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommunicationDataRequest extends BaseCommunicationDataRequest {

    @Valid
    private List<CreateContactPurposeRequest> contactPurposes;

    @Valid
    private List<CreateContactPersonRequest> contactPersons;

    @Valid
    private List<CreateCommunicationContactRequest> communicationContacts;

    public CreateCommunicationDataRequest(EditCommunicationDataRequest request) {
        this.setContactTypeName(request.getContactTypeName());
        this.setAddress(request.getAddress());
        this.setCommunicationContacts(request.getCommunicationContacts().stream().map(CreateCommunicationContactRequest::new).toList());
        this.setStatus(request.getStatus());
        this.setContactPurposes(request.getContactPurposes().stream().map(CreateContactPurposeRequest::new).toList());
        this.setContactPersons(request.getContactPersons().stream().map(CreateContactPersonRequest::new).toList());
    }

    public static List<CreateCommunicationDataRequest> getCreateCommunicationDataRequest(List<EditCommunicationDataRequest> editCommunicationDataRequests) {
        List<CreateCommunicationDataRequest> createCommunicationDataRequests = new ArrayList<>();
        for (EditCommunicationDataRequest request : editCommunicationDataRequests) {
            createCommunicationDataRequests.add(new CreateCommunicationDataRequest(request));
        }
        return createCommunicationDataRequests;
    }
}
