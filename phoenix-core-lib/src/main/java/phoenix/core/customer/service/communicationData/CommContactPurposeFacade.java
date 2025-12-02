package phoenix.core.customer.service.communicationData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommContactPurposes;
import phoenix.core.customer.model.entity.customer.communication.CustomerCommunications;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.communicationData.contactPurpose.CreateContactPurposeRequest;
import phoenix.core.customer.model.request.communicationData.contactPurpose.EditContactPurposeRequest;
import phoenix.core.customer.model.response.customer.communicationData.ContactPurposeBasicInfo;
import phoenix.core.customer.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import phoenix.core.customer.repository.nomenclature.customer.ContactPurposeRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static phoenix.core.customer.model.enums.customer.Status.ACTIVE;
import static phoenix.core.customer.model.enums.customer.Status.DELETED;

@Slf4j
@Service("coreCommContactPurposeService")
@RequiredArgsConstructor
public class CommContactPurposeFacade {
    private final CustomerCommContactPurposesRepository commContactPurposesRepository;
    private final ContactPurposeRepository contactPurposesRepository;

    protected List<ContactPurposeBasicInfo> getCommContactPurposeBasicInfoByCommDataIdAndStatuses(Long commDataId,
                                                                                                  List<Status> statuses) {
        return commContactPurposesRepository.getBasicInfoByCustomerCommIdAndStatuses(commDataId, statuses);
    }

    protected void createContactPurposes(CustomerCommunications communicationData,
                                         List<CreateContactPurposeRequest> contactPurposes,
                                         List<CustomerCommContactPurposes> tempContactPurposesList,
                                         List<String> exceptions) {
        if (!CollectionUtils.isEmpty(contactPurposes)) {
            for (CreateContactPurposeRequest request : contactPurposes) {
                if (request.getStatus().equals(DELETED)) {
                    log.error("Cannot save contact purpose with status DELETED");
                    exceptions.add("Cannot save contact purpose with status DELETED");
                }

                if (!contactPurposesRepository.existsByIdAndStatus(request.getContactPurposeId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("Active contact purpose not found, ID: " + request.getContactPurposeId());
                    exceptions.add("Active contact purpose not found, ID: " + request.getContactPurposeId());
                }

                if (communicationData == null) {
                    log.error("Communication data object is null, cannot add contact purpose ID: " + request.getContactPurposeId());
                    exceptions.add("Communication data object is null, cannot add contact purpose ID: " + request.getContactPurposeId());
                    continue;
                }

                tempContactPurposesList.add(createContactPurpose(communicationData.getId(), request));
            }
        }
    }

    protected void editContactPurposes(Long customerCommunicationsId,
                                     List<EditContactPurposeRequest> contactPurposes,
                                     List<CustomerCommContactPurposes> tempContactPurposesList,
                                     List<String> exceptions) {
        if (CollectionUtils.isEmpty(contactPurposes)) {
            contactPurposes = Collections.emptyList();
        }

        List<CustomerCommContactPurposes> dbContactPurposes = commContactPurposesRepository
                .findByCustomerCommId(customerCommunicationsId, List.of(ACTIVE));

        List<Long> requestIds = contactPurposes.stream().map(EditContactPurposeRequest::getId).toList();

        for (EditContactPurposeRequest request : contactPurposes) {
            if (request.getId() == null) {
                if (request.getStatus().equals(DELETED)) {
                    log.error("Cannot save contact purpose with status DELETED");
                    exceptions.add("Cannot save contact purpose with status DELETED");
                }

                if (!contactPurposesRepository.existsByIdAndStatus(request.getContactPurposeId(), NomenclatureItemStatus.ACTIVE)) {
                    log.error("Active contact purpose not found, ID: " + request.getContactPurposeId());
                    exceptions.add("Active contact purpose not found, ID: " + request.getContactPurposeId());
                }

                tempContactPurposesList.add(createContactPurpose(customerCommunicationsId, new CreateContactPurposeRequest(request)));
            }
        }

        for (CustomerCommContactPurposes purpose : dbContactPurposes) {
            if (!requestIds.contains(purpose.getId())) {
                if (!purpose.getStatus().equals(DELETED)) {
                    tempContactPurposesList.add(deleteContactPurpose(customerCommunicationsId, purpose));
                }
            }
        }
    }

    private CustomerCommContactPurposes createContactPurpose(Long commDataId,
                                                             CreateContactPurposeRequest request) {
        CustomerCommContactPurposes contactPurpose = new CustomerCommContactPurposes();
        contactPurpose.setContactPurposeId(request.getContactPurposeId());
        contactPurpose.setStatus(request.getStatus());
        contactPurpose.setCustomerCommunicationsId(commDataId);
        // TODO: 17.01.23 set actual system user id later
        contactPurpose.setSystemUserId("test");
        contactPurpose.setCreateDate(LocalDateTime.now());
        return contactPurpose;
    }

    private CustomerCommContactPurposes deleteContactPurpose(Long commDataId,
                                                             CustomerCommContactPurposes dbPurpose) {
        CustomerCommContactPurposes contactPurpose = new CustomerCommContactPurposes();
        contactPurpose.setId(dbPurpose.getId());
        contactPurpose.setContactPurposeId(dbPurpose.getContactPurposeId());
        contactPurpose.setStatus(DELETED);
        contactPurpose.setCustomerCommunicationsId(commDataId);
        contactPurpose.setSystemUserId(dbPurpose.getSystemUserId());
        contactPurpose.setCreateDate(dbPurpose.getCreateDate());
        // TODO: 17.01.23 set actual system user id later
        contactPurpose.setModifySystemUserId("test");
        contactPurpose.setModifyDate(LocalDateTime.now());
        return contactPurpose;
    }
}
