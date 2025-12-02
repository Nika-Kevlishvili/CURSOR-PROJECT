package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.enums.contract.express.ExpressCommunicationTypes;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.request.customer.communicationData.CustomerCommAddressRequest;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactDetailedResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerCommunicationsDetailedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ExpressContractCommunicationsRequest {
    private Long id;
    @Valid
    @NotNull(message = "customer.communications.address-address can not be null!;")
    private CustomerCommAddressRequest address;
    @NotNull(message = "customer.communications.contactRequests-contact requests can not be null!;")
    private List<@Valid ExpressContractCommunicationContactRequest> contactRequests;
    @NotNull(message = "customer.communications.communicationTypes - communication types can not be null;")
    private ExpressCommunicationTypes communicationTypes;

    public boolean equalsResponse(CustomerCommunicationsDetailedResponse response) {

        List<ContactDetailedResponse> communicationContacts = response.getCommunicationContacts();
        if (communicationContacts == null) {
            return false;
        }
        if (contactRequests.size() != communicationContacts.size()) return false;
        Map<CustomerCommContactTypes, List<ExpressContractCommunicationContactRequest>> groupedTypes = contactRequests.stream().collect(Collectors.groupingBy(ExpressContractCommunicationContactRequest::getContactType));
        for (ContactDetailedResponse communicationContact : communicationContacts) {
            List<ExpressContractCommunicationContactRequest> contactComRequest = groupedTypes.get(communicationContact.getContactType());
            if (contactComRequest == null) return false;
            if(contactComRequest.stream().map(ExpressContractCommunicationContactRequest::getContactValue).noneMatch(x->x.equals(communicationContact.getContactValue()))) return false;

        }
        return address.equalsResponse(response.getAddress());
    }
}
