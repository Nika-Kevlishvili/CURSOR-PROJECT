package bg.energo.phoenix.model.request.contract.express;

import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import lombok.Data;
@Data
public class ExpressContractCommunicationContactRequest {
    //@NotNull(message = "communicationData.communicationContacts.contactType-Create communication contact request: contact type must not be null;")
    private CustomerCommContactTypes contactType;

    //@NotEmpty(message = "communicationData.communicationContacts.contactValue-Create communication contact request: contact value must not be null;")
    private String contactValue;

}
