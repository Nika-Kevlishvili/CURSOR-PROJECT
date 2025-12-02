package phoenix.core.customer.model.response.customer.communicationData;

import lombok.Data;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunications;

import java.util.List;

@Data
public class CommunicationDataResponse {

    private Long id;

    private String nameOfContactType;

    private List<ContactPurposeBasicInfo> contactPurposes;

    private Boolean foreignAddress;

    private LocalAddressInfo localAddressInfo;

    private ForeignAddressInfo foreignAddressInfo;

    private List<ContactBasicInfo> contacts;

    private List<ContactPersonBasicInfo> contactPersons;

    public CommunicationDataResponse(Boolean foreignAddress,
                                     ForeignAddressInfo foreignAddressInfo,
                                     LocalAddressInfo localAddressInfo,
                                     CustomerCommunications communicationData,
                                     List<ContactPurposeBasicInfo> contactPurposes,
                                     List<ContactBasicInfo> contacts,
                                     List<ContactPersonBasicInfo> contactPersons) {
        this.id = communicationData.getId();
        this.nameOfContactType = communicationData.getContactTypeName();
        this.foreignAddress = foreignAddress;
        this.foreignAddressInfo = foreignAddressInfo;
        this.localAddressInfo = localAddressInfo;
        this.contactPurposes = contactPurposes;
        this.contacts = contacts;
        this.contactPersons = contactPersons;
    }
}
