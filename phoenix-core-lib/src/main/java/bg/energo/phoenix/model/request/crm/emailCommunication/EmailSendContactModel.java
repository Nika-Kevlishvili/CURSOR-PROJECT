package bg.energo.phoenix.model.request.crm.emailCommunication;

import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomer;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationCustomerContact;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EmailSendContactModel {
    private EmailCommunicationCustomer emailCommunicationCustomer;
    private List<EmailCommunicationCustomerContact> emailCommunicationCustomerContacts;
}
