package bg.energo.phoenix.service.customer.customerCommunications;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPerson;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.customer.Title;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.CreateContactPersonRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.EditContactPersonRequest;
import bg.energo.phoenix.model.response.customer.communicationData.ContactPersonBasicInfo;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPersonRepository;
import bg.energo.phoenix.repository.nomenclature.customer.TitleRepository;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.customer.Status.ACTIVE;
import static bg.energo.phoenix.model.enums.customer.Status.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommContactPersonService {
    private final CustomerCommContactPersonRepository contactPersonRepository;
    private final TitleRepository titleRepository;

    /**
     * <h2>Retrieve Customer Communications Persons</h2>
     *
     * @param commDataId ID of {@link CustomerCommunications}
     * @param statuses   {@link List<Status> List&lt;Status&gt;} list of requested statuses
     * @return {@link ContactPersonBasicInfo}
     */
    protected List<ContactPersonBasicInfo> getCommContactPersonBasicInfoByCommDataIdAndStatuses(Long commDataId,
                                                                                                List<Status> statuses) {
        return contactPersonRepository.getBasicInfoByCustomerCommIdAndStatuses(commDataId, statuses);
    }

    /**
     * <h2>Create Contact Persons for Customer Communications</h2>
     * Validations are checked if nomenclatures are active.
     *
     * @param customerCommunications {@link CustomerCommunications}
     * @param contactPersons         {@link List<CreateContactPersonRequest> List&lt;CreateContactPersonRequest&gt;}
     * @param tempContactPersonsList {@link List<CustomerCommContactPerson> List&lt;CustomerCommContactPerson&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions             list of errors which is populated in case of exceptions or validation violations
     */
    protected void createContactPersons(CustomerCommunications customerCommunications,
                                        List<CreateContactPersonRequest> contactPersons,
                                        List<CustomerCommContactPerson> tempContactPersonsList,
                                        List<String> exceptions,
                                        int commDataIndex) {
        if (!CollectionUtils.isEmpty(contactPersons)) {
            for (int i = 0; i < contactPersons.size(); i++) {
                CreateContactPersonRequest request = contactPersons.get(i);
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].contactPersons[%s].status-Cannot save contact person with status DELETED;".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].contactPersons[%s].status-Cannot save contact person with status DELETED;".formatted(commDataIndex, i));
                }

                if (!titleRepository.existsByIdAndStatus(request.getTitleId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("communicationData[%s].contactPersons[%s].titleId-Active title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    exceptions.add("communicationData[%s].contactPersons[%s].titleId-Active title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    continue;
                }

                if (customerCommunications == null) {
                    log.error("communicationData[%s].contactPersons[%s]-Communication data object is null, cannot add contact person with name: [%s];".formatted(commDataIndex, i, request.getName()));
                    exceptions.add("communicationData[%s].contactPersons[%s].name-Communication data object is null, cannot add contact person with name: [%s];".formatted(commDataIndex, i, request.getName()));
                    continue;
                }

                tempContactPersonsList.add(createContactPerson(customerCommunications.getId(), request));
            }
        }
    }

    protected void createContactPersonForNewVersion(CustomerCommunications customerCommunications,
                                                    CustomerCommunications oldCommunications,
                                                    List<EditContactPersonRequest> contactPersons,
                                                    List<CustomerCommContactPerson> tempContactPersonsList,
                                                    List<String> exceptions,
                                                    int commDataIndex) {
        Map<Long, CustomerCommContactPerson> customerCommContactPersonMap;
        if (oldCommunications!=null) {
            customerCommContactPersonMap = contactPersonRepository.findByCustomerCommIdAndStatuses(oldCommunications.getId(), List.of(ACTIVE)).stream()
                    .collect(Collectors.toMap(CustomerCommContactPerson::getId, j -> j));
        }else {
            customerCommContactPersonMap=new HashMap<>();
        }
        if (!CollectionUtils.isEmpty(contactPersons)) {
            for (int i = 0; i < contactPersons.size(); i++) {
                EditContactPersonRequest request = contactPersons.get(i);
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].contactPersons[%s].status-Cannot save contact person with status DELETED;".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].contactPersons[%s].status-Cannot save contact person with status DELETED;".formatted(commDataIndex, i));
                }

                if (!titleRepository.existsByIdAndStatusIn(request.getTitleId(), getTitleStatuses(customerCommContactPersonMap, request.getId(), request.getTitleId()))) {
                    log.error("communicationData[%s].contactPersons[%s].titleId-Active title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    exceptions.add("communicationData[%s].contactPersons[%s].titleId-Active title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    continue;
                }

                if (customerCommunications == null) {
                    log.error("communicationData[%s].contactPersons[%s]-Communication data object is null, cannot add contact person with name: [%s];".formatted(commDataIndex, i, request.getName()));
                    exceptions.add("communicationData[%s].contactPersons[%s].name-Communication data object is null, cannot add contact person with name: [%s];".formatted(commDataIndex, i, request.getName()));
                    continue;
                }

                tempContactPersonsList.add(createContactPerson(customerCommunications.getId(), new CreateContactPersonRequest(request)));
            }
        }
    }

    private List<NomenclatureItemStatus> getTitleStatuses(Map<Long, CustomerCommContactPerson> customerCommContactPersonMap, Long personId, Long titleId) {
        CustomerCommContactPerson customerCommContactPerson = customerCommContactPersonMap.get(personId);
        if (customerCommContactPerson != null && customerCommContactPerson.getTitleId().equals(titleId)) {
            return List.of(NomenclatureItemStatus.ACTIVE, INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    /**
     * <h2>Edit Contact Persons for Customer Communications</h2>
     * Validations are checked if nomenclatures are active/inactive and if nomenclatures are inactive
     * - whether they are different from the persisted {@link CustomerCommContactPerson}'s nomenclatures.
     *
     * @param customerCommunicationsId ID of {@link CustomerCommunications}
     * @param contactPersons           {@link List<EditContactPersonRequest> List&lt;EditContactPersonRequest&gt;}
     * @param tempContactPersonsList   {@link List<CustomerCommContactPerson> List&lt;CustomerCommContactPerson&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions               list of errors which is populated in case of exceptions or validation violations
     */
    protected void editContactPersons(Long customerCommunicationsId,
                                      List<EditContactPersonRequest> contactPersons,
                                      List<CustomerCommContactPerson> tempContactPersonsList,
                                      List<String> exceptions,
                                      int commDataIndex) {
        if (CollectionUtils.isEmpty(contactPersons)) {
            contactPersons = Collections.emptyList();
        }

        List<Long> dbContactPersons = contactPersonRepository
                .findByCustomerCommIdAndStatuses(customerCommunicationsId, List.of(ACTIVE))
                .stream().map(CustomerCommContactPerson::getId)
                .toList();

        for (int i = 0; i < contactPersons.size(); i++) {
            EditContactPersonRequest request = contactPersons.get(i);
            if (request.getStatus().equals(DELETED)) {
                log.error("communicationData[%s].contactPersons[%s].status-Cannot save contact person with status DELETED;".formatted(commDataIndex, i));
                exceptions.add("communicationData[%s].contactPersons[%s].status-Cannot save contact person with status DELETED;".formatted(commDataIndex, i));
            }

            if (request.getId() == null) {
                if (!titleRepository.existsByIdAndStatus(request.getTitleId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("communicationData[%s].contactPersons[%s].titleId-Active title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    exceptions.add("communicationData[%s].contactPersons[%s].titleId-Active title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    continue;
                }

                tempContactPersonsList.add(createContactPerson(customerCommunicationsId, new CreateContactPersonRequest(request)));
            } else {
                Optional<CustomerCommContactPerson> contactPersonOptional = contactPersonRepository
                        .findByIdAndStatuses(request.getId(), List.of(ACTIVE));
                if (contactPersonOptional.isEmpty()) {
                    log.error("communicationData[%s].contactPersons[%s].id-Active contact person not found, ID: [%s];".formatted(commDataIndex, i, request.getId()));
                    exceptions.add("communicationData[%s].contactPersons[%s].id-Active contact person not found, ID: [%s];".formatted(commDataIndex, i, request.getId()));
                    continue;
                }

                CustomerCommContactPerson contactPerson = contactPersonOptional.get();

                Optional<Title> titleOptional = titleRepository
                        .findByIdAndStatuses(request.getTitleId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
                if (titleOptional.isEmpty()) {
                    log.error("communicationData[%s].contactPersons[%s].titleId-Active or inactive title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    exceptions.add("communicationData[%s].contactPersons[%s].titleId-Active or inactive title not found, ID: [%s];".formatted(commDataIndex, i, request.getTitleId()));
                    continue;
                }

                if (titleOptional.get().getStatus().equals(INACTIVE)) {
                    if (!contactPerson.getTitleId().equals(request.getTitleId())) {
                        log.error("communicationData[%s].contactPersons[%s].titleId-Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex, i));
                        exceptions.add("communicationData[%s].contactPersons[%s].titleId-Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(commDataIndex, i));
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

    /**
     * <h2>Delete Contact Persons</h2>
     * Cycle through persisted {@link CustomerCommContactPerson}s and delete them if not found in the provided request.
     *
     * @param contactPersons         {@link List<EditContactPersonRequest> List&lt;EditContactPersonRequest&gt;}
     * @param dbContactPersons       {@link List<Long> List&lt;Long&gt;} IDs of the persisted contact persons
     * @param tempContactPersonsList {@link List<CustomerCommContactPerson> List&lt;CustomerCommContactPerson&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions             list of errors which is populated in case of exceptions or validation violations
     */
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

    /**
     * <h2>Create Contact Person</h2>
     * Populate {@link CustomerCommContactPerson} object
     *
     * @param commDataId ID of {@link CustomerCommunications}
     * @param request    {@link CreateContactPersonRequest}
     * @return {@link CustomerCommContactPerson}
     */
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
        contactPerson.setBirthDate(request.getBirthDate() == null ? null : LocalDate.parse(request.getBirthDate()));
        contactPerson.setAdditionalInfo(request.getAdditionalInformation());
        contactPerson.setStatus(request.getStatus());
        contactPerson.setCustomerCommunicationsId(commDataId);
        return contactPerson;
    }

    /**
     * <h2>Edit Contact Person</h2>
     * Populate {@link CustomerCommContactPerson} object
     *
     * @param commDataId      ID of {@link CustomerCommunications}
     * @param dbContactPerson persisted {@link CustomerCommContactPerson}
     * @param request         {@link EditContactPersonRequest}
     * @return {@link CustomerCommContactPerson}
     */
    private CustomerCommContactPerson editContactPerson(Long commDataId,
                                                        CustomerCommContactPerson dbContactPerson,
                                                        EditContactPersonRequest request) {
        if (!StringUtils.equals(request.getName(), EPBFinalFields.GDPR)) {
            dbContactPerson.setName(request.getName());
        }
        if (!StringUtils.equals(request.getMiddleName(), EPBFinalFields.GDPR)) {
            dbContactPerson.setMiddleName(request.getMiddleName());
        }
        if (!StringUtils.equals(request.getSurname(), EPBFinalFields.GDPR)) {
            dbContactPerson.setSurname(request.getSurname());
        }
        if (!StringUtils.equals(request.getBirthDate(), EPBFinalFields.GDPR) && request.getBirthDate() != null) {
            dbContactPerson.setBirthDate(LocalDate.parse(request.getBirthDate()));
        }


        dbContactPerson.setTitleId(request.getTitleId());
        dbContactPerson.setJobPosition(request.getJobPosition());
        dbContactPerson.setPositionHeldFrom(request.getPositionHeldFrom());
        dbContactPerson.setPositionHeldTo(request.getPositionHeldTo());

        dbContactPerson.setAdditionalInfo(request.getAdditionalInformation());
        dbContactPerson.setStatus(request.getStatus());
        dbContactPerson.setCustomerCommunicationsId(commDataId);
        return dbContactPerson;
    }

    /**
     * <h2>Delete Contact Person</h2>
     * Delete {@link CustomerCommContactPerson} if not already deleted
     *
     * @param dbContactPerson persisted {@link CustomerCommContactPerson}
     * @return {@link CustomerCommunicationContacts}
     */
    private CustomerCommContactPerson deleteContactPerson(CustomerCommContactPerson dbContactPerson) {
        dbContactPerson.setStatus(DELETED);
        return dbContactPerson;
    }
}
