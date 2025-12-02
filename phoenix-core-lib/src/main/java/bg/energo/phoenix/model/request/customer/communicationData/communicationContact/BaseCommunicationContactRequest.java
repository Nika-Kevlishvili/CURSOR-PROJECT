package bg.energo.phoenix.model.request.customer.communicationData.communicationContact;

import bg.energo.phoenix.model.customAnotations.customer.BaseCommunicationContractValidator;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@BaseCommunicationContractValidator
public class BaseCommunicationContactRequest {

    @NotNull(message = "communicationData.communicationContacts.sendSms-Create communication contact request: sendSms must not be null;")
    private Boolean sendSms;

    private Long platformId;

    @NotNull(message = "communicationData.communicationContacts.status-Create communication contact request: status must not be null;")
    private Status status;

    @NotNull(message = "communicationData.communicationContacts.contactType-Create communication contact request: contact type must not be null;")
    private CustomerCommContactTypes contactType;

    @NotEmpty(message = "communicationData.communicationContacts.contactValue-Create communication contact request: contact value must not be null;")
    private String contactValue;

}
