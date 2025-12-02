package bg.energo.phoenix.service.customer.customerCommunications;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.CreateCommunicationContactRequest;
import bg.energo.phoenix.model.request.customer.communicationData.communicationContact.EditCommunicationContactRequest;
import bg.energo.phoenix.model.response.customer.communicationData.ContactBasicInfo;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.nomenclature.customer.PlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes.MOBILE_NUMBER;
import static bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes.OTHER_PLATFORM;
import static bg.energo.phoenix.model.enums.customer.Status.ACTIVE;
import static bg.energo.phoenix.model.enums.customer.Status.DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommContactService {
    private final CustomerCommunicationContactsRepository contactsRepository;
    private final PlatformRepository platformRepository;

    /**
     * <h2>Retrieve Customer Communications Contacts</h2>
     *
     * @param commDataId ID of {@link CustomerCommunications}
     * @param statuses   {@link List<Status> List&lt;Status&gt;} list of requested statuses
     * @return {@link ContactBasicInfo}
     */
    protected List<ContactBasicInfo> getCommContactBasicInfoByCommDataIdAndStatuses(Long commDataId,
                                                                                    List<Status> statuses) {
        return contactsRepository.getBasicInfoByCustomerCommIdAndStatuses(commDataId, statuses);
    }

    /**
     * <h2>Create Contacts for Customer Communications</h2>
     * Validations are checked if nomenclatures are active.
     * Only one mobile phone in this {@link CustomerCommunications} can have selected SMS.
     *
     * @param customerCommunications {@link CustomerCommunications}
     * @param communicationContacts  {@link List<CreateCommunicationContactRequest> List&lt;CreateCommunicationContactRequest&gt;}
     * @param tempContactsList       {@link List<CustomerCommunicationContacts> List&lt;CustomerCommunicationContacts&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions             list of errors which is populated in case of exceptions or validation violations
     */
    protected void createContacts(CustomerCommunications customerCommunications,
                                  List<CreateCommunicationContactRequest> communicationContacts,
                                  List<CustomerCommunicationContacts> tempContactsList,
                                  List<String> exceptions,
                                  int commDataIndex) {
        int smsContactCount = 0;

        if (!CollectionUtils.isEmpty(communicationContacts)) {
            for (int i = 0; i < communicationContacts.size(); i++) {
                CreateCommunicationContactRequest request = communicationContacts.get(i);
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].communicationContacts[%s].status-Cannot save contact with status DELETED;".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].communicationContacts[%s].status-Cannot save contact with status DELETED;".formatted(commDataIndex, i));
                }

                if (request.getContactType().equals(OTHER_PLATFORM) && !platformRepository.existsByIdAndStatus(request.getPlatformId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("communicationData[%s].communicationContacts[%s].platformId-Active platform not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getPlatformId()));
                    exceptions.add("communicationData[%s].communicationContacts[%s].platformId-Active platform not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getPlatformId()));
                }


                if (request.getContactType().equals(MOBILE_NUMBER) && request.getSendSms()) {
                    smsContactCount++;
                    if (smsContactCount > 1) {
                        log.error("communicationData[%s].communicationContacts[%s].sendSms-Only one mobile phone in this Communication data can have selected SMS, contact value: [%s];"
                                .formatted(commDataIndex, i, request.getContactValue()));
                        exceptions.add("communicationData[%s].communicationContacts[%s].sendSms-Only one mobile phone in this Communication data can have selected SMS, contact value: [%s];"
                                .formatted(commDataIndex, i, request.getContactValue()));
                    }
                }

                if (customerCommunications == null) {
                    log.error("communicationData[%s].communicationContacts[%s]-Communication data object is null, cannot add contact: [%s];"
                            .formatted(commDataIndex, i, request.getContactValue()));
                    exceptions.add("communicationData[%s].communicationContacts[%s].contactValue-Communication data object is null, cannot add contact: [%s];"
                            .formatted(commDataIndex, i, request.getContactValue()));
                    continue;
                }

                tempContactsList.add(createContact(customerCommunications.getId(), request));
            }
        }
    }

    protected void createContactsForNewVersion(CustomerCommunications customerCommunications,
                                               CustomerCommunications oldCommunications,
                                               List<EditCommunicationContactRequest> communicationContacts,
                                               List<CustomerCommunicationContacts> tempContactsList,
                                               List<String> exceptions,
                                               int commDataIndex) {
        int smsContactCount = 0;
        Map<Long, CustomerCommunicationContacts> contactsMap;
        if (oldCommunications != null) {
            contactsMap = contactsRepository.findByCustomerCommIdAndStatuses(oldCommunications.getId(), List.of(ACTIVE))
                    .stream().collect(Collectors.toMap(CustomerCommunicationContacts::getId, j -> j));
        } else {
            contactsMap = new HashMap<>();
        }
        if (!CollectionUtils.isEmpty(communicationContacts)) {
            for (int i = 0; i < communicationContacts.size(); i++) {
                EditCommunicationContactRequest request = communicationContacts.get(i);
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].communicationContacts[%s].status-Cannot save contact with status DELETED;".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].communicationContacts[%s].status-Cannot save contact with status DELETED;".formatted(commDataIndex, i));
                }

                if (request.getContactType().equals(OTHER_PLATFORM) && !platformRepository.existsByIdAndStatusIn(request.getPlatformId(), getPlatformStatus(contactsMap, request.getId(), request.getPlatformId()))) {
                    log.error("communicationData[%s].communicationContacts[%s].platformId-Active platform not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getPlatformId()));
                    exceptions.add("communicationData[%s].communicationContacts[%s].platformId-Active platform not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getPlatformId()));
                }


                if (request.getContactType().equals(MOBILE_NUMBER) && request.getSendSms()) {
                    smsContactCount++;
                    if (smsContactCount > 1) {
                        log.error("communicationData[%s].communicationContacts[%s].sendSms-Only one mobile phone in this Communication data can have selected SMS, contact value: [%s];"
                                .formatted(commDataIndex, i, request.getContactValue()));
                        exceptions.add("communicationData[%s].communicationContacts[%s].sendSms-Only one mobile phone in this Communication data can have selected SMS, contact value: [%s];"
                                .formatted(commDataIndex, i, request.getContactValue()));
                    }
                }

                if (customerCommunications == null) {
                    log.error("communicationData[%s].communicationContacts[%s]-Communication data object is null, cannot add contact: [%s];"
                            .formatted(commDataIndex, i, request.getContactValue()));
                    exceptions.add("communicationData[%s].communicationContacts[%s].contactValue-Communication data object is null, cannot add contact: [%s];"
                            .formatted(commDataIndex, i, request.getContactValue()));
                    continue;
                }

                tempContactsList.add(createContact(customerCommunications.getId(), new CreateCommunicationContactRequest(request)));
            }
        }
    }

    private List<NomenclatureItemStatus> getPlatformStatus(Map<Long, CustomerCommunicationContacts> contactsMap, Long contactId, Long platformId) {
        CustomerCommunicationContacts customerCommunicationContacts = contactsMap.get(contactId);
        if (customerCommunicationContacts != null && customerCommunicationContacts.getPlatformId().equals(platformId)) {
            return List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    /**
     * <h2>Edit Contacts for Customer Communications</h2>
     * Validations are checked if nomenclatures are active. {@link CustomerCommunicationContacts} cannot be edited,
     * user has to delete a contact and add it anew each time, so this editing operation combines adding and deleting contacts.
     * Only one mobile phone in this {@link CustomerCommunications} can have selected SMS.
     *
     * @param customerCommunicationsId ID of {@link CustomerCommunications}
     * @param communicationContacts    {@link List<EditCommunicationContactRequest> List&lt;EditCommunicationContactRequest&gt;}
     * @param tempContactsList         {@link List<CustomerCommunicationContacts> List&lt;CustomerCommunicationContacts&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions               list of errors which is populated in case of exceptions or validation violations
     */
    protected void editContacts(Long customerCommunicationsId,
                                List<EditCommunicationContactRequest> communicationContacts,
                                List<CustomerCommunicationContacts> tempContactsList,
                                List<String> exceptions,
                                int commDataIndex) {
        if (CollectionUtils.isEmpty(communicationContacts)) {
            communicationContacts = Collections.emptyList();
        }

        List<CustomerCommunicationContacts> dbContacts = contactsRepository
                .findByCustomerCommIdAndStatuses(customerCommunicationsId, List.of(ACTIVE));

        CustomerCommunicationContacts currentSmsContact = null;
        if (CollectionUtils.isNotEmpty(dbContacts)) {
            currentSmsContact = dbContacts.stream()
                    .filter(contact -> contact.getContactType().equals(MOBILE_NUMBER) && contact.isSendSms())
                    .findFirst()
                    .orElse(null);
        }

        List<Long> requestIds = communicationContacts
                .stream()
                .map(EditCommunicationContactRequest::getId).toList();

        int smsContactCount = 0;

        for (int i = 0; i < communicationContacts.size(); i++) {
            EditCommunicationContactRequest request = communicationContacts.get(i);
            if (request.getContactType().equals(MOBILE_NUMBER) && request.getSendSms()) {
                smsContactCount++;
            }

            if (request.getId() == null) {
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].communicationContacts[%s].status-Cannot save contact with status DELETED".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].communicationContacts[%s].status-Cannot save contact with status DELETED".formatted(commDataIndex, i));
                }

                if (request.getContactType().equals(OTHER_PLATFORM) && !platformRepository.existsByIdAndStatus(request.getPlatformId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("communicationData[%s].communicationContacts[%s].platformId-Active platform not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getPlatformId()));
                    exceptions.add("communicationData[%s].communicationContacts[%s].platformId-Active platform not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getPlatformId()));
                }

                tempContactsList.add(createContact(customerCommunicationsId, new CreateCommunicationContactRequest(request)));
            }

            if (smsContactCount > 1) {
                log.error("communicationData[%s].communicationContacts[%s].sendSms-Only one mobile phone in this Communication data can have selected SMS, contact value: [%s];"
                        .formatted(commDataIndex, i, request.getContactValue()));
                exceptions.add("communicationData[%s].communicationContacts[%s].sendSms-Only one mobile phone in this Communication data can have selected SMS, contact value: [%s];"
                        .formatted(commDataIndex, i, request.getContactValue()));
                break;
            }
        }

        EditCommunicationContactRequest providedSmsContact = communicationContacts
                .stream()
                .filter(contact -> contact.getContactType().equals(MOBILE_NUMBER) && contact.getSendSms())
                .findFirst()
                .orElse(null);

        for (CustomerCommunicationContacts contact : dbContacts) {
            if (providedSmsContact != null && currentSmsContact != null && currentSmsContact.getId().equals(contact.getId())
                    && (providedSmsContact.getId() == null || !providedSmsContact.getId().equals(currentSmsContact.getId()))) {
                // if user has provided a new SMS contact and there is already a contact with SMS selected, then unselect it and save
                contact.setSendSms(false);
            }

            if (!requestIds.contains(contact.getId())) {
                if (!contact.getStatus().equals(DELETED)) {
                    contact.setStatus(DELETED);
                }
            }

            tempContactsList.add(contact);
        }
    }

    /**
     * <h2>Create Contact</h2>
     * Populate {@link CustomerCommunicationContacts} object
     *
     * @param commDataId ID of {@link CustomerCommunications}
     * @param request    {@link CreateCommunicationContactRequest}
     * @return {@link CustomerCommunicationContacts}
     */
    private CustomerCommunicationContacts createContact(Long commDataId,
                                                        CreateCommunicationContactRequest request) {
        CustomerCommunicationContacts contact = new CustomerCommunicationContacts();
        contact.setContactType(request.getContactType());
        contact.setContactValue(request.getContactValue());
        contact.setPlatformId(request.getPlatformId());
        contact.setSendSms(request.getSendSms());
        contact.setStatus(request.getStatus());
        contact.setCustomerCommunicationsId(commDataId);
        return contact;
    }
}
