package phoenix.core.customer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.entity.customer.Manager;
import phoenix.core.customer.model.entity.nomenclature.customer.RepresentationMethod;
import phoenix.core.customer.model.entity.nomenclature.customer.Title;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.request.manager.CreateManagerRequest;
import phoenix.core.customer.model.request.manager.EditManagerRequest;
import phoenix.core.customer.model.response.customer.manager.ManagerBasicInfo;
import phoenix.core.customer.model.response.customer.manager.ManagerResponse;
import phoenix.core.customer.repository.customer.CustomerDetailsRepository;
import phoenix.core.customer.repository.customer.ManagerRepository;
import phoenix.core.customer.repository.nomenclature.customer.RepresentationMethodRepository;
import phoenix.core.customer.repository.nomenclature.customer.TitleRepository;
import phoenix.core.exception.DomainEntityNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static phoenix.core.customer.model.enums.customer.Status.ACTIVE;
import static phoenix.core.customer.model.enums.customer.Status.DELETED;
import static phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service("coreManagerService")
@RequiredArgsConstructor
@Validated
public class ManagerFacade {
    private final CustomerDetailsRepository customerDetailsRepository;
    private final TitleRepository titleRepository;
    private final RepresentationMethodRepository representationMethodRepository;
    private final ManagerRepository managerRepository;

    // for viewing managers list on create customer page
    public List<ManagerBasicInfo> previewByCustomerDetailId(CustomerDetails customerDetails, List<String> exceptions) {
        if (customerDetails == null) {
            log.debug("Customer details object is null, cannot retrieve related customers");
            exceptions.add("Customer details object is null, cannot retrieve related customers");
            return null;
        }

        if (!customerDetailsRepository.existsById(customerDetails.getId())) {
            log.error("Customer detail not found, ID: " + customerDetails.getId());
            exceptions.add("Customer detail not found, ID: " + customerDetails.getId());
            return null;
        }

        log.debug("Fetching managers for customer detail ID: {}", customerDetails.getId());
        return managerRepository.getManagersByCustomerDetailId(customerDetails.getId(), Status.ACTIVE);
    }

    // for viewing a manager in its own modal
    public ManagerResponse getDetailedManagerById(Long managerId) {
        log.debug("Fetching manager by ID: {}", managerId);

        Manager manager = managerRepository
                .findManagerByIdAndStatuses(managerId, List.of(ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active manager not found, ID: " + managerId));

        return new ManagerResponse(manager);
    }

    // for adding managers to customer when creating
    @Transactional
    public void addManagers(List<CreateManagerRequest> requests,
                            CustomerDetails customerDetails,
                            List<String> exceptions) {
        if (!CollectionUtils.isEmpty(requests)) {
            if (customerDetails == null) {
                log.error("Customer details is null, cannot add managers");
                exceptions.add("Customer details is null, cannot add managers");
                return;
            }

            log.debug("Adding managers list: {} to customer detail ID: {}", requests, customerDetails.getId());

            if (!customerDetailsRepository.existsById(customerDetails.getId())) {
                log.error("Customer detail not found, ID: " + customerDetails.getId());
                exceptions.add("Customer detail not found, ID: " + customerDetails.getId());
                return;
            }

            List<Manager> tempManagersList = new ArrayList<>();

            for (CreateManagerRequest request : requests) {
                add(tempManagersList, request, customerDetails, exceptions);
            }

            if (exceptions.isEmpty()) {
                managerRepository.saveAll(tempManagersList);
            }
        }
    }

    // for editing managers of customer

    @Transactional
    public void editManagers(List<EditManagerRequest> request, CustomerDetails customerDetails, List<String> exceptions) {
        log.debug("Editing managers list to customer detail ID: {}", customerDetails.getId());
        processManagers(request, customerDetails, exceptions);
    }

    // processing each manager, deciding validation and saving strategy depending on added or edited, deleting removed items
    private void processManagers(List<EditManagerRequest> requests, CustomerDetails customerDetails, List<String> exceptions) {
        if (requests == null) {
            requests = Collections.emptyList();
        }

        if (!customerDetailsRepository.existsById(customerDetails.getId())) {
            log.error("Customer detail not found, ID: " + customerDetails.getId());
            exceptions.add("Customer detail not found, ID: " + customerDetails.getId());
            return;
        }

        List<Long> managerIdsByCustomerDetailId = managerRepository
                .getManagerIdsByCustomerDetailId(customerDetails.getId(), Status.ACTIVE);

        List<Manager> tempManagersList = new ArrayList<>();

        for (EditManagerRequest managerRequest : requests) {
            if (managerRequest.getId() == null) {
                add(tempManagersList, new CreateManagerRequest(managerRequest), customerDetails, exceptions);
            } else {
                edit(tempManagersList, managerRequest, customerDetails.getId(), exceptions);
            }
        }

        if (exceptions.isEmpty()) {
            managerRepository.saveAll(tempManagersList);
            deleteRemovedManagers(requests, managerIdsByCustomerDetailId, exceptions);
        }
    }

    private void add(List<Manager> tempManagersList,
                     CreateManagerRequest request,
                     CustomerDetails customerDetails,
                     List<String> exceptions) {
        Optional<Title> titleOptional = titleRepository
                .findByIdAndStatuses(request.getTitleId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (titleOptional.isEmpty()) {
            log.error("Active title not found, ID: " + request.getTitleId());
            exceptions.add("Active title not found, ID: " + request.getTitleId());
            return;
        }

        Optional<RepresentationMethod> representationMethodOptional = representationMethodRepository
                .findByIdAndStatuses(request.getRepresentationMethodId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (representationMethodOptional.isEmpty()) {
            log.error("Active representation method not found, ID: " + request.getRepresentationMethodId());
            exceptions.add("Active representation method not found, ID: " + request.getRepresentationMethodId());
            return;
        }

        saveNewManager(
                request,
                titleOptional.get(),
                representationMethodOptional.get(),
                customerDetails.getId(),
                tempManagersList
        );
    }

    private void edit(List<Manager> tempManagersList, EditManagerRequest managerRequest, Long customerDetailId, List<String> exceptions) {
        Optional<Manager> managerOptional = managerRepository.findManagerByIdAndStatuses(managerRequest.getId(), List.of(ACTIVE));
        if (managerOptional.isEmpty()) {
            log.error("Request ID " + managerRequest.getId() + ": Manager not found, ID: " + managerRequest.getId());
            exceptions.add("Request ID " + managerRequest.getId() + ": Manager not found, ID: " + managerRequest.getId());
            return;
        }

        Manager dbManager = managerOptional.get();
        if (!dbManager.getCustomerDetailId().equals(customerDetailId)) {
            log.error("Request ID " + managerRequest.getId() + ": Cannot change current customer detail ID: " + dbManager.getCustomerDetailId()
                    + "with the requested different customer detail ID: " + customerDetailId);
            exceptions.add("Request ID " + managerRequest.getId() + ": Cannot change current customer detail ID: " + dbManager.getCustomerDetailId()
                    + "with the requested different customer detail ID: " + customerDetailId);
        }

        Optional<Title> titleOptional = titleRepository
                .findByIdAndStatuses(managerRequest.getTitleId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (titleOptional.isEmpty()) {
            log.error("Active or inactive title not found, ID: " + managerRequest.getTitleId());
            exceptions.add("Active or inactive title not found, ID: " + managerRequest.getTitleId());
            return;
        }

        Optional<RepresentationMethod> representationMethodOptional = representationMethodRepository
                .findByIdAndStatuses(managerRequest.getRepresentationMethodId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (representationMethodOptional.isEmpty()) {
            log.error("Active or inactive representation method not found, ID: " + managerRequest.getRepresentationMethodId());
            exceptions.add("Active or inactive representation method not found, ID: " + managerRequest.getRepresentationMethodId());
            return;
        }

        if (titleOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbManager.getTitle().getId().equals(managerRequest.getTitleId())) {
                log.error("Request ID " + managerRequest.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Request ID " + managerRequest.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        if (representationMethodOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbManager.getRepresentationMethod().getId().equals(managerRequest.getRepresentationMethodId())) {
                log.error("Request ID " + managerRequest.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
                exceptions.add("Request ID " + managerRequest.getId() + ": Cannot save object with different INACTIVE nomenclature item if it already has such");
            }
        }

        saveEditedManager(
                managerRequest,
                managerOptional.get(),
                titleOptional.get(),
                representationMethodOptional.get(),
                customerDetailId,
                tempManagersList
        );
    }

    private void saveNewManager(CreateManagerRequest managerRequest,
                                Title title,
                                RepresentationMethod representationMethod,
                                Long customerDetailId,
                                List<Manager> tempManagersList) {
        Manager manager = new Manager(managerRequest, customerDetailId);
        manager.setTitle(title);
        manager.setRepresentationMethod(representationMethod);
        manager.setCustomerDetailId(customerDetailId);
        // TODO: 04.01.23 set systemUserId later
        manager.setSystemUserId("test");
        manager.setCreateDate(LocalDateTime.now());
        tempManagersList.add(manager);
    }
    
    private void saveEditedManager(EditManagerRequest managerRequest,
                                   Manager dbManager,
                                   Title title,
                                   RepresentationMethod representationMethod,
                                   Long customerDetailId,
                                   List<Manager> tempManagersList) {
        Manager manager = new Manager(managerRequest, customerDetailId);
        manager.setSystemUserId(dbManager.getSystemUserId());
        manager.setCreateDate(dbManager.getCreateDate());
        manager.setTitle(title);
        manager.setRepresentationMethod(representationMethod);
        manager.setCustomerDetailId(customerDetailId);
        // TODO: 04.01.23 set modify systemUserId later
        manager.setModifySystemUserId("modifier");
        manager.setModifyDate(LocalDateTime.now());
        tempManagersList.add(manager);
    }

    private void deleteRemovedManagers(List<EditManagerRequest> request,
                                       List<Long> persistedManagers,
                                       List<String> exceptions) {
        if (!persistedManagers.isEmpty()) {
            List<Long> requestManagerIds = request
                    .stream()
                    .map(EditManagerRequest::getId)
                    .toList();

            for (Long managerId : persistedManagers) {
                if (!requestManagerIds.contains(managerId)) {
                    deleteManager(managerId, exceptions);
                }
            }
        }
    }

    private void deleteManager(Long managerId, List<String> exceptions) {
        log.debug("Deleting manager with ID: {}", managerId);

        Optional<Manager> managerOptional = managerRepository.findById(managerId);
        if (managerOptional.isEmpty()) {
            log.error("Manager not found, ID: " + managerId);
            exceptions.add("Manager not found, ID: " + managerId);
            return;
        }

        Manager manager = managerOptional.get();

        if (!manager.getStatus().equals(DELETED)) {
            manager.setStatus(DELETED);
            // TODO: 03.01.23 set modify system user id
            manager.setModifySystemUserId("modifier");
            manager.setModifyDate(LocalDateTime.now());
            managerRepository.save(manager);
        }

    }
}
