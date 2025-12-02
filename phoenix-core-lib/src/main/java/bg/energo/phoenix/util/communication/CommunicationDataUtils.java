package bg.energo.phoenix.util.communication;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommunicationDataUtils {

    public static void checkCommunicationEmailAndNumber(Long communicationDataId, List<String> messages, String message, CustomerCommunicationContactsRepository communicationContactsRepository) {
        List<CustomerCommunicationContacts> contactsList = communicationContactsRepository.findByCustomerCommIdAndStatuses(communicationDataId, List.of(Status.ACTIVE));

        Set<CustomerCommContactTypes> contactTypes = new HashSet<>();
        contactTypes.add(CustomerCommContactTypes.MOBILE_NUMBER);
        contactTypes.add(CustomerCommContactTypes.EMAIL);

        for (CustomerCommunicationContacts contacts : contactsList) {
            contactTypes.remove(contacts.getContactType());
        }
        if (!contactTypes.isEmpty()) {
            messages.add(message);
        }
    }
}

