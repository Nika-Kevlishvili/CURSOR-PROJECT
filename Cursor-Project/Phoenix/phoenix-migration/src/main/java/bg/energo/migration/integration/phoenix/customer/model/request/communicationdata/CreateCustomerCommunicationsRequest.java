package bg.energo.migration.integration.phoenix.customer.model.request.communicationdata;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.commaddress.CustomerCommAddressRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.communicationcontacts.CreateCommunicationContactRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.contactpersons.CreateContactPersonRequest;
import bg.energo.migration.integration.phoenix.customer.model.request.communicationdata.contactpurposes.CreateContactPurposeRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerCommunicationsRequest {
    private String contactTypeName;
    private CustomerCommAddressRequest address;
    private Status status;
    private List<CreateContactPurposeRequest> contactPurposes;
    private List<CreateContactPersonRequest> contactPersons;//empty
    private List<CreateCommunicationContactRequest> communicationContacts;
}
