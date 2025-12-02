package bg.energo.phoenix.service.customer.customerCommunications;

import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.CreateContactPurposeRequest;
import bg.energo.phoenix.model.request.customer.communicationData.contactPurpose.EditContactPurposeRequest;
import bg.energo.phoenix.model.response.customer.communicationData.ContactPurposeBasicInfo;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.nomenclature.customer.ContactPurposeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.customer.Status.ACTIVE;
import static bg.energo.phoenix.model.enums.customer.Status.DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommContactPurposeService {
    private final CustomerCommContactPurposesRepository commContactPurposesRepository;
    private final ContactPurposeRepository contactPurposesRepository;

    /**
     * <h2>Retrieve Customer Communications Contact Purposes</h2>
     *
     * @param commDataId ID of {@link CustomerCommunications}
     * @param statuses   {@link List<Status> List&lt;Status&gt;} list of requested statuses
     * @return {@link ContactPurposeBasicInfo}
     */
    protected List<ContactPurposeBasicInfo> getCommContactPurposeBasicInfoByCommDataIdAndStatuses(Long commDataId,
                                                                                                  List<Status> statuses) {
        return commContactPurposesRepository.getBasicInfoByCustomerCommIdAndStatuses(commDataId, statuses);
    }

    /**
     * <h2>Create Contact Purposes for Customer Communications</h2>
     * Validations are checked if nomenclatures are active.
     *
     * @param customerCommunications  {@link CustomerCommunications}
     * @param contactPurposes         {@link List<CustomerCommContactPurposes> List&lt;CustomerCommContactPurposes&gt;}
     * @param tempContactPurposesList {@link List<CustomerCommContactPurposes> List&lt;CustomerCommContactPurposes&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions              list of errors which is populated in case of exceptions or validation violations
     */
    protected void createContactPurposes(CustomerCommunications customerCommunications,
                                         List<CreateContactPurposeRequest> contactPurposes,
                                         List<CustomerCommContactPurposes> tempContactPurposesList,
                                         List<String> exceptions,
                                         int commDataIndex) {
        if (!CollectionUtils.isEmpty(contactPurposes)) {
            for (int i = 0; i < contactPurposes.size(); i++) {
                CreateContactPurposeRequest request = contactPurposes.get(i);
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].contactPurposes[%s].status-Cannot save contact purpose with status DELETED;".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].contactPurposes[%s].status-Cannot save contact purpose with status DELETED;".formatted(commDataIndex, i));
                }

                if (!contactPurposesRepository.existsByIdAndStatus(request.getContactPurposeId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("communicationData[%s].contactPurposes[%s].contactPurposeId-Active contact purpose not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                    exceptions.add("communicationData[%s].contactPurposes[%s].contactPurposeId-Active contact purpose not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                }

                if (customerCommunications == null) {
                    log.error("communicationData[%s].contactPurposes[%s]-Communication data object is null, cannot add contact purpose ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                    exceptions.add("communicationData[%s].contactPurposes[%s].contactPurposeId-Communication data object is null, cannot add contact purpose ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                    continue;
                }

                tempContactPurposesList.add(createContactPurpose(customerCommunications.getId(), request));
            }
        }
    }

    protected void createContactPurposesForNewVersion(
            CustomerCommunications customerCommunications,
            CustomerCommunications oldCommunications,
            List<EditContactPurposeRequest> contactPurposes,
            List<CustomerCommContactPurposes> tempContactPurposesList,
            List<String> exceptions,
            int commDataIndex) {
        Set<Long> purposeIds;
        if (oldCommunications != null) {
            purposeIds = commContactPurposesRepository.findByCustomerCommId(oldCommunications.getId(), List.of(ACTIVE))
                    .stream().map(CustomerCommContactPurposes::getContactPurposeId).collect(Collectors.toSet());
        }else {
            purposeIds=new HashSet<>();
        }
        if (!CollectionUtils.isEmpty(contactPurposes)) {
            for (int i = 0; i < contactPurposes.size(); i++) {
                EditContactPurposeRequest request = contactPurposes.get(i);
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].contactPurposes[%s].status-Cannot save contact purpose with status DELETED;".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].contactPurposes[%s].status-Cannot save contact purpose with status DELETED;".formatted(commDataIndex, i));
                }

                if (!contactPurposesRepository.existsByIdAndStatusIn(request.getContactPurposeId(), getPurposeStatuses(purposeIds, request.getContactPurposeId()))) {
                    log.error("communicationData[%s].contactPurposes[%s].contactPurposeId-Active contact purpose not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                    exceptions.add("communicationData[%s].contactPurposes[%s].contactPurposeId-Active contact purpose not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                }

                if (customerCommunications == null) {
                    log.error("communicationData[%s].contactPurposes[%s]-Communication data object is null, cannot add contact purpose ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                    exceptions.add("communicationData[%s].contactPurposes[%s].contactPurposeId-Communication data object is null, cannot add contact purpose ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                    continue;
                }

                tempContactPurposesList.add(createContactPurpose(customerCommunications.getId(), new CreateContactPurposeRequest(request)));
            }
        }
    }

    private List<NomenclatureItemStatus> getPurposeStatuses(Set<Long> purposeIds, Long contactPurposeId) {
        if (purposeIds.contains(contactPurposeId)) {
            return List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    /**
     * <h2>Edit Contact Purposes for Customer Communications</h2>
     * Validations are checked if nomenclatures are active. {@link CustomerCommContactPurposes} cannot be edited,
     * user has to delete a contact purpose and add it anew each time, so this editing operation combines adding and deleting contacts.
     *
     * @param customerCommunicationsId ID of {@link CustomerCommunications}
     * @param contactPurposes          {@link List<EditContactPurposeRequest> List&lt;EditContactPurposeRequest&gt;}
     * @param tempContactPurposesList  {@link List<CustomerCommContactPurposes> List&lt;CustomerCommContactPurposes&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param exceptions               list of errors which is populated in case of exceptions or validation violations
     */
    protected void editContactPurposes(Long customerCommunicationsId,
                                       List<EditContactPurposeRequest> contactPurposes,
                                       List<CustomerCommContactPurposes> tempContactPurposesList,
                                       List<String> exceptions,
                                       int commDataIndex) {
        if (CollectionUtils.isEmpty(contactPurposes)) {
            contactPurposes = Collections.emptyList();
        }

        List<CustomerCommContactPurposes> dbContactPurposes = commContactPurposesRepository
                .findByCustomerCommId(customerCommunicationsId, List.of(ACTIVE));

        List<Long> requestIds = contactPurposes.stream().map(EditContactPurposeRequest::getId).toList();

        for (int i = 0; i < contactPurposes.size(); i++) {
            EditContactPurposeRequest request = contactPurposes.get(i);
            if (request.getId() == null) {
                if (request.getStatus().equals(DELETED)) {
                    log.error("communicationData[%s].contactPurposes[%s].status-Cannot save contact purpose with status DELETED;".formatted(commDataIndex, i));
                    exceptions.add("communicationData[%s].contactPurposes[%s].status-Cannot save contact purpose with status DELETED;".formatted(commDataIndex, i));
                }

                if (!contactPurposesRepository.existsByIdAndStatus(request.getContactPurposeId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("Active contact purpose not found, ID: " + request.getContactPurposeId() + ";");
                    exceptions.add("communicationData[%s].contactPurposes[%s].contactPurposeId-Active contact purpose not found, ID: [%s];"
                            .formatted(commDataIndex, i, request.getContactPurposeId()));
                }

                tempContactPurposesList.add(createContactPurpose(customerCommunicationsId, new CreateContactPurposeRequest(request)));
            }
        }

        for (CustomerCommContactPurposes purpose : dbContactPurposes) {
            if (!requestIds.contains(purpose.getId())) {
                if (!purpose.getStatus().equals(DELETED)) {
                    tempContactPurposesList.add(deleteContactPurpose(purpose));
                }
            }
        }
    }

    /**
     * <h2>Create Contact Purpose</h2>
     * Populate {@link CustomerCommContactPurposes} object
     *
     * @param commDataId ID of {@link CustomerCommunications}
     * @param request    {@link CreateContactPurposeRequest}
     * @return {@link CustomerCommunicationContacts}
     */
    private CustomerCommContactPurposes createContactPurpose(Long commDataId,
                                                             CreateContactPurposeRequest request) {
        CustomerCommContactPurposes contactPurpose = new CustomerCommContactPurposes();
        contactPurpose.setContactPurposeId(request.getContactPurposeId());
        contactPurpose.setStatus(request.getStatus());
        contactPurpose.setCustomerCommunicationsId(commDataId);
        return contactPurpose;
    }

    /**
     * <h2>Delete Contact</h2>
     * Delete {@link CustomerCommContactPurposes} if not already deleted
     *
     * @param dbPurpose persisted {@link CustomerCommContactPurposes}
     * @return {@link CustomerCommunicationContacts}
     */
    private CustomerCommContactPurposes deleteContactPurpose(CustomerCommContactPurposes dbPurpose) {
        dbPurpose.setStatus(DELETED);
        return dbPurpose;
    }
}
