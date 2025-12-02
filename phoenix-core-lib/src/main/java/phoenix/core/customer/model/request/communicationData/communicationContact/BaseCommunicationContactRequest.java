package phoenix.core.customer.model.request.communicationData.communicationContact;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import phoenix.core.customer.model.enums.customer.CustomerCommContactTypes;
import phoenix.core.customer.model.enums.customer.Status;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseCommunicationContactRequest {

    private Boolean sendSms;

    private Long platformId;

    private Status status;

    private CustomerCommContactTypes contactType;

    private String contactValue;

}
