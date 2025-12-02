package bg.energo.phoenix.model.response.customer.communicationData.detailed;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.communicationData.ForeignAddressInfo;
import bg.energo.phoenix.model.response.customer.communicationData.LocalAddressInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCommunicationsDetailedResponse {
    private Long id;
    private Status status;
    private String contactTypeName;
    private CustomerAddress address;
    private List<ContactPurposeDetailedResponse> contactPurposes;
    private List<ContactPersonDetailedResponse> contactPersons;
    private List<ContactDetailedResponse> communicationContacts;
    private String concatPurposes;

    public CustomerCommunicationsDetailedResponse(CustomerCommunications customerCommunications,
                                                  Boolean foreignAddress,
                                                  ForeignAddressInfo foreignAddressInfo,
                                                  LocalAddressInfo localAddressInfo,
                                                  List<ContactPurposeDetailedResponse> contactPurposes,
                                                  List<ContactPersonDetailedResponse> contactPersons,
                                                  List<ContactDetailedResponse> communicationContacts) {
        this.id = customerCommunications.getId();
        this.status = customerCommunications.getStatus();
        this.contactTypeName = customerCommunications.getContactTypeName();
        this.address = new CustomerAddress(
                foreignAddress,
                foreignAddressInfo,
                localAddressInfo,
                customerCommunications.getStreetNumber(),
                customerCommunications.getAddressAdditionalInfo(),
                customerCommunications.getBlock(),
                customerCommunications.getEntrance(),
                customerCommunications.getFloor(),
                customerCommunications.getApartment(),
                customerCommunications.getMailbox()
        );
        this.contactPurposes = contactPurposes;
        this.contactPersons = contactPersons;
        this.communicationContacts = communicationContacts;
    }
}
