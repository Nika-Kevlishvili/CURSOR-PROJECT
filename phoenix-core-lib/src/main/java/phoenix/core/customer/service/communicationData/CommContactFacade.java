package phoenix.core.customer.service.communicationData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunicationContacts;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunications;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.communicationData.communicationContact.CreateCommunicationContactRequest;
import phoenix.core.customer.model.request.communicationData.communicationContact.EditCommunicationContactRequest;
import phoenix.core.customer.model.response.customer.communicationData.ContactBasicInfo;
import phoenix.core.customer.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import phoenix.core.customer.repository.nomenclature.customer.PlatformRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static phoenix.core.customer.model.enums.customer.CustomerCommContactTypes.MOBILE_NUMBER;
import static phoenix.core.customer.model.enums.customer.CustomerCommContactTypes.OTHER_PLATFORM;
import static phoenix.core.customer.model.enums.customer.Status.ACTIVE;
import static phoenix.core.customer.model.enums.customer.Status.DELETED;

@Slf4j
@Service("coreCommContactService")
@RequiredArgsConstructor
public class CommContactFacade {
    private final CustomerCommunicationContactsRepository contactsRepository;
    private final PlatformRepository platformRepository;

    protected List<ContactBasicInfo> getCommContactBasicInfoByCommDataIdAndStatuses(Long commDataId,
                                                                                    List<Status> statuses) {
        return contactsRepository.getBasicInfoByCustomerCommIdAndStatuses(commDataId, statuses);
    }

    protected void createContacts(CustomerCommunications communicationData,
                                  List<CreateCommunicationContactRequest> communicationContacts,
                                  List<CustomerCommunicationContacts> tempContactsList,
                                  List<String> exceptions) {

        if (!CollectionUtils.isEmpty(communicationContacts)) {
            int smsContactCount = 0;

            for (CreateCommunicationContactRequest request : communicationContacts) {
                if (request.getStatus().equals(DELETED)) {
                    log.error("Cannot save contact with status DELETED");
                    exceptions.add("Cannot save contact with status DELETED");
                }

                if (request.getContactType().equals(OTHER_PLATFORM) && !platformRepository.existsByIdAndStatus(request.getPlatformId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("Active platform not found, ID: " + request.getPlatformId());
                    exceptions.add("Active platform not found, ID: " + request.getPlatformId());
                }


                if (request.getContactType().equals(MOBILE_NUMBER) && request.getSendSms()) {
                    smsContactCount++;
                    if (smsContactCount > 1) {
                        log.error("Only one mobile phone in this Communication data can have selected SMS, contact value: " + request.getContactValue());
                        exceptions.add("Only one mobile phone in this Communication data can have selected SMS, contact value: " + request.getContactValue());
                    }
                }

                if (communicationData == null) {
                    log.error("Communication data object is null, cannot add contact: " + request.getContactValue());
                    exceptions.add("Communication data object is null, cannot add contact: " + request.getContactValue());
                    continue;
                }

                tempContactsList.add(createContact(communicationData.getId(), request));
            }
        }
    }

    protected void editContacts(Long customerCommunicationsId,
                              List<EditCommunicationContactRequest> communicationContacts,
                              List<CustomerCommunicationContacts> tempContactsList,
                              List<String> exceptions) {
        if (CollectionUtils.isEmpty(communicationContacts)) {
            communicationContacts = Collections.emptyList();
        }

        List<CustomerCommunicationContacts> dbContacts = contactsRepository
                .findByCustomerCommIdAndStatuses(customerCommunicationsId, List.of(ACTIVE));

        List<Long> requestIds = communicationContacts
                .stream()
                .map(EditCommunicationContactRequest::getId).toList();

        int smsContactCount = 0;

        for (EditCommunicationContactRequest request : communicationContacts) {
            if (request.getContactType().equals(MOBILE_NUMBER) && request.getSendSms()) {
                smsContactCount++;
            }

            if (request.getId() == null) {
                if (request.getStatus().equals(DELETED)) {
                    log.error("Cannot save contact with status DELETED");
                    exceptions.add("Cannot save contact with status DELETED");
                }

                if (request.getContactType().equals(OTHER_PLATFORM) && !platformRepository.existsByIdAndStatus(request.getPlatformId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("Active platform not found, ID: " + request.getPlatformId());
                    exceptions.add("Active platform not found, ID: " + request.getPlatformId());
                }

                tempContactsList.add(createContact(customerCommunicationsId, new CreateCommunicationContactRequest(request)));
            }

            if (smsContactCount > 1) {
                log.error("Only one mobile phone in this Communication data can have selected SMS, contact value: " + request.getContactValue());
                exceptions.add("Only one mobile phone in this Communication data can have selected SMS, contact value: " + request.getContactValue());
                break;
            }
        }

        for (CustomerCommunicationContacts contact : dbContacts) {
            if (!requestIds.contains(contact.getId())) {
                if (!contact.getStatus().equals(DELETED)) {
                    tempContactsList.add(deleteContact(customerCommunicationsId, contact));
                }
            }
        }
    }

    private CustomerCommunicationContacts createContact(Long commDataId,
                                                        CreateCommunicationContactRequest request) {
        CustomerCommunicationContacts contact = new CustomerCommunicationContacts();
        contact.setContactType(request.getContactType());
        contact.setContactValue(request.getContactValue());
        contact.setPlatformId(request.getPlatformId());
        contact.setSendSms(request.getSendSms());
        contact.setStatus(request.getStatus());
        contact.setCustomerCommunicationsId(commDataId);
        // TODO: 17.01.23 set actual system user id later
        contact.setSystemUserId("test");
        contact.setCreateDate(LocalDateTime.now());
        return contact;
    }

    private CustomerCommunicationContacts deleteContact(Long commDataId,
                                                        CustomerCommunicationContacts dbContact) {
        CustomerCommunicationContacts contacts = new CustomerCommunicationContacts();
        contacts.setId(dbContact.getId());
        contacts.setContactType(dbContact.getContactType());
        contacts.setContactValue(dbContact.getContactValue());
        contacts.setPlatformId(dbContact.getPlatformId());
        contacts.setSendSms(dbContact.isSendSms());
        contacts.setStatus(DELETED);
        contacts.setCustomerCommunicationsId(commDataId);
        contacts.setSystemUserId(dbContact.getSystemUserId());
        contacts.setCreateDate(dbContact.getCreateDate());
        contacts.setModifyDate(LocalDateTime.now());
        // TODO: 17.01.23 set actual system user id later
        contacts.setModifySystemUserId("test");
        return contacts;
    }
}
