package phoenix.core.customer.service.communicationData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommContactPerson;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunications;
import phoenix.core.customer.model.entity.nomenclature.customer.Title;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.communicationData.contactPerson.CreateContactPersonRequest;
import phoenix.core.customer.model.request.communicationData.contactPerson.EditContactPersonRequest;
import phoenix.core.customer.model.response.customer.communicationData.ContactPersonBasicInfo;
import phoenix.core.customer.repository.customer.communicationData.CustomerCommContactPersonRepository;
import phoenix.core.customer.repository.nomenclature.customer.TitleRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static phoenix.core.customer.model.enums.customer.Status.ACTIVE;
import static phoenix.core.customer.model.enums.customer.Status.DELETED;
import static phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service("coreCommContactPersonService")
@RequiredArgsConstructor
public class CommContactPersonFacade {
    private final CustomerCommContactPersonRepository contactPersonRepository;
    private final TitleRepository titleRepository;

    protected List<ContactPersonBasicInfo> getCommContactPersonBasicInfoByCommDataIdAndStatuses(Long commDataId,
                                                                                                List<Status> statuses) {
        return contactPersonRepository.getBasicInfoByCustomerCommIdAndStatuses(commDataId, statuses);
    }

    protected void createContactPersons(CustomerCommunications communicationData,
                                      List<CreateContactPersonRequest> contactPersons,
                                      List<CustomerCommContactPerson> tempContactPersonsList,
                                      List<String> exceptions) {
        if (!CollectionUtils.isEmpty(contactPersons)) {
            for (CreateContactPersonRequest request : contactPersons) {
                if (request.getStatus().equals(DELETED)) {
                    log.error("Cannot save contact person with status DELETED");
                    exceptions.add("Cannot save contact person with status DELETED");
                }

                if (!titleRepository.existsByIdAndStatus(request.getTitleId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("Active title not found, ID: " + request.getTitleId());
                    exceptions.add("Active title not found, ID: " + request.getTitleId());
                    continue;
                }

                if (communicationData == null) {
                    log.error("Communication data object is null, cannot add contact person with name: " + request.getName());
                    exceptions.add("Communication data object is null, cannot add contact person with name: " + request.getName());
                    continue;
                }

                tempContactPersonsList.add(createContactPerson(communicationData.getId(), request));
            }
        }
    }

    protected void editContactPersons(Long customerCommunicationsId,
                                      List<EditContactPersonRequest> contactPersons,
                                      List<CustomerCommContactPerson> tempContactPersonsList,
                                      List<String> exceptions) {
        if (CollectionUtils.isEmpty(contactPersons)) {
            contactPersons = Collections.emptyList();
        }

        List<Long> dbContactPersons = contactPersonRepository
                .findByCustomerCommIdAndStatuses(customerCommunicationsId, List.of(ACTIVE))
                .stream().map(CustomerCommContactPerson::getId)
                .toList();

        for (EditContactPersonRequest request : contactPersons) {
            if (request.getStatus().equals(DELETED)) {
                log.error("Cannot save contact person with status DELETED");
                exceptions.add("Cannot save contact person with status DELETED");
            }

            if (request.getId() == null) {
                if (!titleRepository.existsByIdAndStatus(request.getTitleId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("Active title not found, ID: " + request.getTitleId());
                    exceptions.add("Active title not found, ID: " + request.getTitleId());
                    continue;
                }

                tempContactPersonsList.add(createContactPerson(customerCommunicationsId, new CreateContactPersonRequest(request)));
            } else {
                Optional<CustomerCommContactPerson> contactPersonOptional = contactPersonRepository
                        .findByIdAndStatuses(request.getId(), List.of(ACTIVE));
                if (contactPersonOptional.isEmpty()) {
                    log.error("Active contact person not found, ID: " + request.getId());
                    exceptions.add("Active contact person not found, ID: " + request.getId());
                    continue;
                }

                CustomerCommContactPerson contactPerson = contactPersonOptional.get();

                Optional<Title> titleOptional = titleRepository
                        .findByIdAndStatuses(request.getTitleId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));
                if (titleOptional.isEmpty()) {
                    log.error("Active or inactive title not found, ID: " + request.getTitleId());
                    exceptions.add("Active or inactive title not found, ID: " + request.getTitleId());
                    continue;
                }

                if (titleOptional.get().getStatus().equals(INACTIVE)) {
                    if (!contactPerson.getTitleId().equals(request.getTitleId())) {
                        log.error("Request ID " + request.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
                        exceptions.add("Request ID " + request.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
                    }
                }

                tempContactPersonsList.add(editContactPerson(customerCommunicationsId, contactPerson, request));
            }
        }

        removeDeletedContactPersons(
                contactPersons,
                dbContactPersons,
                tempContactPersonsList,
                exceptions
        );
    }

    private void removeDeletedContactPersons(List<EditContactPersonRequest> contactPersons,
                                             List<Long> dbContactPersons,
                                             List<CustomerCommContactPerson> tempContactPersonsList,
                                             List<String> exceptions) {
        if (exceptions.isEmpty()) {
            if (!dbContactPersons.isEmpty()) {
                List<Long> requestContactPersonIds = contactPersons
                        .stream()
                        .map(EditContactPersonRequest::getId)
                        .toList();

                for (Long contactPersonId : dbContactPersons) {
                    if (!requestContactPersonIds.contains(contactPersonId)) {
                        CustomerCommContactPerson dbContactPerson = contactPersonRepository.findById(contactPersonId).get();

                        if (!dbContactPerson.getStatus().equals(DELETED)) {
                            tempContactPersonsList.add(deleteContactPerson(dbContactPerson));
                        }
                    }
                }
            }
        }
    }

    private CustomerCommContactPerson createContactPerson(Long commDataId,
                                                          CreateContactPersonRequest request) {
        CustomerCommContactPerson contactPerson = new CustomerCommContactPerson();
        contactPerson.setName(request.getName());
        contactPerson.setMiddleName(request.getMiddleName());
        contactPerson.setSurname(request.getSurname());
        contactPerson.setTitleId(request.getTitleId());
        contactPerson.setJobPosition(request.getJobPosition());
        contactPerson.setPositionHeldFrom(request.getPositionHeldFrom());
        contactPerson.setPositionHeldTo(request.getPositionHeldTo());
        contactPerson.setBirthDate(request.getBirthDate());
        contactPerson.setAdditionalInfo(request.getAdditionalInformation());
        contactPerson.setStatus(request.getStatus());
        contactPerson.setCustomerCommunicationsId(commDataId);
        // TODO: 17.01.23 set actual system user id later
        contactPerson.setSystemUserId("test");
        contactPerson.setCreateDate(LocalDateTime.now());
        return contactPerson;
    }

    private CustomerCommContactPerson editContactPerson(Long commDataId,
                                                        CustomerCommContactPerson dbContactPerson,
                                                        EditContactPersonRequest request) {
        CustomerCommContactPerson contactPerson = new CustomerCommContactPerson();
        contactPerson.setId(dbContactPerson.getId());
        contactPerson.setName(request.getName());
        contactPerson.setMiddleName(request.getMiddleName());
        contactPerson.setSurname(request.getSurname());
        contactPerson.setTitleId(request.getTitleId());
        contactPerson.setJobPosition(request.getJobPosition());
        contactPerson.setPositionHeldFrom(request.getPositionHeldFrom());
        contactPerson.setPositionHeldTo(request.getPositionHeldTo());
        contactPerson.setBirthDate(request.getBirthDate());
        contactPerson.setAdditionalInfo(request.getAdditionalInformation());
        contactPerson.setStatus(request.getStatus());
        contactPerson.setCustomerCommunicationsId(commDataId);
        // TODO: 17.01.23 set actual system user id later
        contactPerson.setSystemUserId(dbContactPerson.getSystemUserId());
        contactPerson.setCreateDate(dbContactPerson.getCreateDate());
        contactPerson.setModifyDate(LocalDateTime.now());
        contactPerson.setModifySystemUserId("test");
        return contactPerson;
    }

    private CustomerCommContactPerson deleteContactPerson(CustomerCommContactPerson dbContactPerson) {
        CustomerCommContactPerson contactPerson = new CustomerCommContactPerson();
        contactPerson.setId(dbContactPerson.getId());
        contactPerson.setName(dbContactPerson.getName());
        contactPerson.setMiddleName(dbContactPerson.getMiddleName());
        contactPerson.setSurname(dbContactPerson.getSurname());
        contactPerson.setTitleId(dbContactPerson.getTitleId());
        contactPerson.setJobPosition(dbContactPerson.getJobPosition());
        contactPerson.setPositionHeldFrom(dbContactPerson.getPositionHeldFrom());
        contactPerson.setPositionHeldTo(dbContactPerson.getPositionHeldTo());
        contactPerson.setBirthDate(dbContactPerson.getBirthDate());
        contactPerson.setAdditionalInfo(dbContactPerson.getAdditionalInfo());
        contactPerson.setStatus(DELETED);
        contactPerson.setCustomerCommunicationsId(dbContactPerson.getCustomerCommunicationsId());
        contactPerson.setSystemUserId(dbContactPerson.getSystemUserId());
        contactPerson.setCreateDate(dbContactPerson.getCreateDate());
        contactPerson.setModifyDate(LocalDateTime.now());
        // TODO: 17.01.23 set actual system user id later
        contactPerson.setModifySystemUserId("test");
        return contactPerson;
    }
}
