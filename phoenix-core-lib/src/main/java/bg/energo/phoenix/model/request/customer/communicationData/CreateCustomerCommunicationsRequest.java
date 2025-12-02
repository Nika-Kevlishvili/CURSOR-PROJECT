package bg.energo.phoenix.model.request.customer.communicationData;

import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.CreateCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.CreateContactPersonRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.CreateContactPurposeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCustomerCommunicationsRequest extends BaseCustomerCommunicationsRequest {

    @Valid
    @Size(min = 1, message = "communicationData.contactPurposes-Customer communications should have at least {min} contact purpose;")
    private List<CreateContactPurposeRequest> contactPurposes;

    @Valid
    private List<CreateContactPersonRequest> contactPersons;

    @Valid
    private List<CreateCommunicationContactRequest> communicationContacts;

    public CreateCustomerCommunicationsRequest(EditCustomerCommunicationsRequest request) {
        this.setContactTypeName(request.getContactTypeName());
        this.setAddress(request.getAddress());
        this.setCommunicationContacts(request.getCommunicationContacts() == null ? Collections.emptyList()
                        : request.getCommunicationContacts().stream().map(CreateCommunicationContactRequest::new).toList());
        this.setStatus(request.getStatus());
        this.setContactPurposes(request.getContactPurposes().stream().map(CreateContactPurposeRequest::new).toList());
        this.setContactPersons(request.getContactPersons() == null ? Collections.emptyList()
                : request.getContactPersons().stream().map(CreateContactPersonRequest::new).toList());
    }

    public static List<CreateCustomerCommunicationsRequest> getCreateCommunicationDataRequest(List<EditCustomerCommunicationsRequest> editCommunicationDataRequests) {
        List<CreateCustomerCommunicationsRequest> createCommunicationDataRequests = new ArrayList<>();
        if (!CollectionUtils.isEmpty(editCommunicationDataRequests)) {
            for (EditCustomerCommunicationsRequest request : editCommunicationDataRequests) {
                createCommunicationDataRequests.add(new CreateCustomerCommunicationsRequest(request));
            }
        }
        return createCommunicationDataRequests;
    }
}
