package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.entity.nomenclature.customer.RepresentationMethod;
import bg.energo.phoenix.model.entity.nomenclature.customer.Title;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.manager.CreateManagerRequest;
import bg.energo.phoenix.model.request.customer.manager.EditManagerRequest;
import bg.energo.phoenix.model.response.customer.manager.ManagerBasicInfo;
import bg.energo.phoenix.model.response.customer.manager.ManagerResponse;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.RepresentationMethodRepository;
import bg.energo.phoenix.repository.nomenclature.customer.TitleRepository;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.customer.Status.ACTIVE;
import static bg.energo.phoenix.model.enums.customer.Status.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerService {
    private final CustomerDetailsRepository customerDetailsRepository;
    private final TitleRepository titleRepository;
    private final RepresentationMethodRepository representationMethodRepository;
    private final ManagerRepository managerRepository;


    /**
     * Retrieves basic information about the managers for the provided {@link CustomerDetails} ID.
     *
     * @param customerDetailId ID of the {@link CustomerDetails} for which the managers are requested.
     * @return list of {@link ManagerBasicInfo} that contains basic information about each {@link Manager}
     */
    public List<ManagerBasicInfo> getManagersByCustomerDetailId(Long customerDetailId) {
        log.debug("Fetching managers for customer detail ID: {}", customerDetailId);
        return managerRepository.getManagersByCustomerDetailId(customerDetailId, Status.ACTIVE);
    }

    /**
     * Retrieves detailed information for the requested {@link Manager} by the {@link Manager}'s ID.
     *
     * @param managerId ID of the requested {@link Manager}.
     * @return {@link ManagerResponse} that contains all the information needed to display the {@link Manager} in its own modal.
     * @throws DomainEntityNotFoundException if no active {@link Manager} is found by the requested ID.
     */
    public ManagerResponse getDetailedManagerById(Long managerId) {
        log.debug("Fetching manager by ID: {}", managerId);

        Manager manager = managerRepository
                .findManagerByIdAndStatuses(managerId, List.of(ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Active manager not found, ID: " + managerId + ";"));

        return new ManagerResponse(manager);
    }

    /**
     * Lists all active {@link Manager}s for presented ID of the {@link CustomerDetails}.
     *
     * @param customerDetailsId version ID of the customer for which the {@link Manager}s are requested.
     * @return {@link List<ManagerResponse> List&lt;ManagerResponse&gt;} that contains extended information about each {@link Manager}
     */
    public List<ManagerResponse> getManagersByCustomerDetailsId(Long customerDetailsId) {
        log.debug("Fetching managers by customer detail ID: {}", customerDetailsId);
        return managerRepository
                .findManagersByCustomerDetailId(customerDetailsId, List.of(ACTIVE))
                .stream()
                .map(ManagerResponse::new)
                .toList();
    }

    /**
     * Adds {@link Manager}s to the provided {@link CustomerDetails} if the request list not empty and exceptions are empty.
     * This method should be used only when creating a new customer or a new version.
     *
     * @param requests        {@link List<CreateManagerRequest> List&lt;CreateManagerRequest&gt;}
     * @param customerDetails a version of the customer to which the managers should be added
     * @param exceptions      list of errors which is populated in case of exceptions or validation violations
     */
    @Transactional
    public void addManagers(List<CreateManagerRequest> requests,
                            CustomerDetails customerDetails,
                            List<String> exceptions) {
        if (CollectionUtils.isNotEmpty(requests)) {
            if (customerDetails == null) {
                log.error("Customer details is null, cannot add managers");
                exceptions.add("%s-Customer details is null, cannot add managers;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR));
                return;
            }

            if (!customerDetailsRepository.existsById(customerDetails.getId())) {
                log.error("Customer detail not found, ID: " + customerDetails.getId());
                exceptions.add("%s-Customer detail not found, ID: %s;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR, customerDetails.getId()));
                return;
            }

            log.debug("Adding managers list: {} to customer detail ID: {}", requests, customerDetails.getId());
            List<Manager> tempManagersList = new ArrayList<>();

            for (int i = 0; i < requests.size(); i++) {
                CreateManagerRequest request = requests.get(i);
                add(tempManagersList, request, customerDetails, exceptions, i);
            }

            if (exceptions.isEmpty()) {
                managerRepository.saveAll(tempManagersList);
            }
        }
    }

    /**
     * This method takes care of GDPR compliance before adding a list of managers to a new version of a customer details object.
     *
     * @param requests   a list of {@link EditManagerRequest}
     * @param newVersion the new version of the customer details object
     * @param exceptions a list of exceptions that may occur during the execution of this method
     * @param oldVersion the old version of the customer details object
     */
    @Transactional
    public void addManagersToNewVersion(List<EditManagerRequest> requests,
                                        CustomerDetails newVersion,
                                        List<String> exceptions,
                                        CustomerDetails oldVersion) {
        if (CollectionUtils.isNotEmpty(requests)) {
            List<Manager> dbManagersList = managerRepository.findManagersByCustomerDetailId(oldVersion.getId(), List.of(ACTIVE));

            Map<Long, Manager> managerMap = dbManagersList.stream().collect(Collectors.toMap(Manager::getId, x -> x));
            log.debug("Adding managers list: {} to customer detail ID: {}", requests, newVersion.getId());
            List<Manager> tempManagersList = new ArrayList<>();
            int index = 0;
            for (EditManagerRequest request : requests) {
                List<NomenclatureItemStatus> statusesToSearch = new ArrayList<>() {{
                    add(NomenclatureItemStatus.ACTIVE);
                }};
                if (request.getId() != null) {
                    Manager dbManager = managerMap.get(request.getId());

                    if (dbManager == null) {
                        log.error("managers[%s].id-Error while fetching manager details for manager ID: [%s];".formatted(index, request.getId()));
                        exceptions.add("managers[%s].id-Error while fetching manager details for manager ID: [%s];".formatted(index, request.getId()));
                        return;
                    }

                    if (StringUtils.equals(request.getPersonalNumber(), EPBFinalFields.GDPR)) {
                        request.setPersonalNumber(dbManager.getPersonalNumber());
                    }

                    if (StringUtils.equals(request.getBirthDate(), EPBFinalFields.GDPR)) {
                        LocalDate birthDate = dbManager.getBirthDate();
                        request.setBirthDate(birthDate == null ? null : birthDate.toString());
                    }
                    statusesToSearch.add(INACTIVE);
                }
                Optional<Title> titleOptional = titleRepository
                        .findByIdAndStatuses(request.getTitleId(), statusesToSearch);
                if (titleOptional.isEmpty()) {
                    log.error("managers[%s].titleId-Active title not found, ID: [%s];".formatted(index, request.getTitleId()));
                    exceptions.add("managers[%s].titleId-Active title not found, ID: [%s];".formatted(index, request.getTitleId()));
                    return;
                }
                RepresentationMethod representationMethod = null;
                if (request.getRepresentationMethodId() != null) {
                    Optional<RepresentationMethod> representationMethodOptional = representationMethodRepository
                            .findByIdAndStatuses(request.getRepresentationMethodId(), statusesToSearch);
                    if (representationMethodOptional.isEmpty()) {
                        log.error("managers[%s].representationMethodId-Active representation method not found, ID: [%s];".formatted(index, request.getRepresentationMethodId()));
                        exceptions.add("managers[%s].representationMethodId-Active representation method not found, ID: [%s];".formatted(index, request.getRepresentationMethodId()));
                        return;
                    }
                    representationMethod = representationMethodOptional.get();
                }
                saveNewManager(
                        new CreateManagerRequest(request),
                        titleOptional.get(),
                        representationMethod,
                        newVersion.getId(),
                        tempManagersList
                );
                index++;
            }


            if (exceptions.isEmpty()) {
                managerRepository.saveAll(tempManagersList);
            }

        }
    }

    /**
     * Combines three operations - editing {@link Manager}s belonging to the provided {@link CustomerDetails} if ID is not null in the request,
     * adding new {@link Manager}s if the ID is null or deleting removed {@link Manager}s.
     *
     * @param requests        {@link List<EditManagerRequest> List&lt;EditManagerRequest&gt;}
     * @param customerDetails a version of the customer for which the managers should be edited
     * @param exceptions      list of errors which is populated in case of exceptions or validation violations
     */
    @Transactional
    public void editManagers(List<EditManagerRequest> requests, CustomerDetails customerDetails, List<String> exceptions) {
        log.debug("Editing managers list to customer detail ID: {}", customerDetails.getId());
        processManagers(requests, customerDetails, exceptions);
    }

    /**
     * Processes each manager, deciding validation and saving strategy
     * depending on the operation - adding, editing or deleting removed items
     *
     * @param requests        {@link List<EditManagerRequest> List&lt;EditManagerRequest&gt;}
     * @param customerDetails a version of the customer for which the managers should be processed
     * @param exceptions      list of errors which is populated in case of exceptions or validation violations
     */
    private void processManagers(List<EditManagerRequest> requests, CustomerDetails customerDetails, List<String> exceptions) {
        if (requests == null) {
            requests = Collections.emptyList();
        }

        if (!customerDetailsRepository.existsById(customerDetails.getId())) {
            log.error("Customer detail not found, ID: " + customerDetails.getId());
            exceptions.add("%s-Customer detail not found, ID: %s;".formatted(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR, customerDetails.getId()));
            return;
        }

        List<Long> managerIdsByCustomerDetailId = managerRepository
                .getManagerIdsByCustomerDetailId(customerDetails.getId(), Status.ACTIVE);

        List<Manager> tempManagersList = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            EditManagerRequest managerRequest = requests.get(i);
            if (managerRequest.getId() == null) {
                add(tempManagersList, new CreateManagerRequest(managerRequest), customerDetails, exceptions, i);
            } else {
                edit(tempManagersList, managerRequest, customerDetails.getId(), exceptions, i);
            }
        }

        if (exceptions.isEmpty()) {
            managerRepository.saveAll(tempManagersList);
            deleteRemovedManagers(requests, managerIdsByCustomerDetailId, exceptions);
        }
    }

    /**
     * Validations for adding a manager include checking if the provided nomenclatures exist and are active.
     *
     * @param tempManagersList {@link List<Manager> List&lt;Manager&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param request          {@link CreateManagerRequest}
     * @param customerDetails  a version of the customer
     * @param exceptions       list of errors which is populated in case of exceptions or validation violations
     */
    private void add(List<Manager> tempManagersList,
                     CreateManagerRequest request,
                     CustomerDetails customerDetails,
                     List<String> exceptions,
                     int index) {
        Optional<Title> titleOptional = titleRepository
                .findByIdAndStatuses(request.getTitleId(), List.of(NomenclatureItemStatus.ACTIVE));
        if (titleOptional.isEmpty()) {
            log.error("managers[%s].titleId-Active title not found, ID: [%s];".formatted(index, request.getTitleId()));
            exceptions.add("managers[%s].titleId-Active title not found, ID: [%s];".formatted(index, request.getTitleId()));
            return;
        }
        RepresentationMethod representationMethod = null;
        if (request.getRepresentationMethodId() != null) {
            Optional<RepresentationMethod> representationMethodOptional = representationMethodRepository
                    .findByIdAndStatuses(request.getRepresentationMethodId(), List.of(NomenclatureItemStatus.ACTIVE));
            if (representationMethodOptional.isEmpty()) {
                log.error("managers[%s].representationMethodId-Active representation method not found, ID: [%s];".formatted(index, request.getRepresentationMethodId()));
                exceptions.add("managers[%s].representationMethodId-Active representation method not found, ID: [%s];".formatted(index, request.getRepresentationMethodId()));
                return;
            }
            representationMethod = representationMethodOptional.get();
        }

        saveNewManager(
                request,
                titleOptional.get(),
                representationMethod,
                customerDetails.getId(),
                tempManagersList
        );
    }

    /**
     * Validations for editing a manager include checking if a persisted {@link Manager} exists with the provided ID,
     * if the persisted {@link Manager} belongs to the provided {@link CustomerDetails},
     * if the nomenclatures exist and are active/inactive and if nomenclatures are inactive - whether they are different
     * from the persisted {@link Manager}s nomenclatures.
     *
     * @param tempManagersList {@link List<Manager> List&lt;Manager&gt;} temporary list in which all processed requests are accumulated and then saved together
     * @param managerRequest   {@link EditManagerRequest}
     * @param customerDetailId a version of the customer
     * @param exceptions       list of errors which is populated in case of exceptions or validation violations
     */
    private void edit(List<Manager> tempManagersList,
                      EditManagerRequest managerRequest,
                      Long customerDetailId,
                      List<String> exceptions,
                      int index) {
        Optional<Manager> managerOptional = managerRepository.findManagerByIdAndStatuses(managerRequest.getId(), List.of(ACTIVE));
        if (managerOptional.isEmpty()) {
            log.error("managers[%s].id-Manager not found, ID: [%s];".formatted(index, managerRequest.getId()));
            exceptions.add("managers[%s].id-Manager not found, ID: [%s];".formatted(index, managerRequest.getId()));
            return;
        }

        Manager dbManager = managerOptional.get();
        if (!dbManager.getCustomerDetailId().equals(customerDetailId)) {
            log.error("managers[%s].id-Cannot change current customer detail ID: [%s] with the requested different customer detail ID: [%s];"
                    .formatted(index, dbManager.getCustomerDetailId(), customerDetailId));
            exceptions.add("managers[%s].id-Cannot change current customer detail ID: [%s] with the requested different customer detail ID: [%s];"
                    .formatted(index, dbManager.getCustomerDetailId(), customerDetailId));
        }

        Optional<Title> titleOptional = titleRepository
                .findByIdAndStatuses(managerRequest.getTitleId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (titleOptional.isEmpty()) {
            log.error("managers[%s].titleId-Active or inactive title not found, ID: [%s];".formatted(index, managerRequest.getTitleId()));
            exceptions.add("managers[%s].titleId-Active or inactive title not found, ID: [%s];".formatted(index, managerRequest.getTitleId()));
            return;
        }

        Optional<RepresentationMethod> representationMethodOptional = representationMethodRepository
                .findByIdAndStatuses(managerRequest.getRepresentationMethodId(), List.of(NomenclatureItemStatus.ACTIVE, INACTIVE));
        if (representationMethodOptional.isEmpty()) {
            log.error("managers[%s].representationMethodId-Active or inactive representation method not found, ID: [%s];"
                    .formatted(index, managerRequest.getRepresentationMethodId()));
            exceptions.add("managers[%s].representationMethodId-Active or inactive representation method not found, ID: [%s];"
                    .formatted(index, managerRequest.getRepresentationMethodId()));
            return;
        }

        if (titleOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbManager.getTitle().getId().equals(managerRequest.getTitleId())) {
                log.error("managers[%s].titleId-Cannot save object with different INACTIVE nomenclature item if it already has such;");
                exceptions.add("managers[%s].titleId-Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(index));
            }
        }

        if (representationMethodOptional.get().getStatus().equals(INACTIVE)) {
            if (!dbManager.getRepresentationMethod().getId().equals(managerRequest.getRepresentationMethodId())) {
                log.error("managers[%s].representationMethodId-Cannot save object with different INACTIVE nomenclature item if it already has such;");
                exceptions.add("managers[%s].representationMethodId-Cannot save object with different INACTIVE nomenclature item if it already has such;".formatted(index));
            }
        }

        saveEditedManager(
                dbManager,
                managerRequest,
                titleOptional.get(),
                representationMethodOptional.get(),
                customerDetailId,
                tempManagersList
        );
    }

    /**
     * Populates {@link Manager} object to be persisted afterwards.
     *
     * @param managerRequest       {@link CreateManagerRequest}
     * @param title                {@link Title}
     * @param representationMethod {@link RepresentationMethod}
     * @param customerDetailId     a version of the customer
     * @param tempManagersList     temporary list in which all processed requests are accumulated and then saved together
     */
    private void saveNewManager(CreateManagerRequest managerRequest,
                                Title title,
                                RepresentationMethod representationMethod,
                                Long customerDetailId,
                                List<Manager> tempManagersList) {
        Manager manager = new Manager(managerRequest, customerDetailId);
        manager.setTitle(title);
        manager.setRepresentationMethod(representationMethod);
        manager.setCustomerDetailId(customerDetailId);
        tempManagersList.add(manager);
    }

    /**
     * Populates {@link Manager} object to be persisted afterwards.
     *
     * @param request              {@link EditManagerRequest}
     * @param title                {@link Title}
     * @param representationMethod {@link RepresentationMethod}
     * @param customerDetailId     a version of the customer
     * @param tempManagersList     temporary list in which all processed requests are accumulated and then saved together
     */
    private void saveEditedManager(Manager manager,
                                   EditManagerRequest request,
                                   Title title,
                                   RepresentationMethod representationMethod,
                                   Long customerDetailId,
                                   List<Manager> tempManagersList) {
        manager.setName(request.getName());
        manager.setMiddleName(request.getMiddleName());
        manager.setSurname(request.getSurname());
        if (!StringUtils.equals(request.getSurname(), EPBFinalFields.GDPR)) {
            manager.setPersonalNumber(request.getPersonalNumber());
        }

        manager.setJobPosition(request.getJobPosition());
        manager.setPositionHeldFrom(request.getPositionHeldFrom());
        manager.setPositionHeldTo(request.getPositionHeldTo());
        if (!StringUtils.equals(request.getBirthDate(), EPBFinalFields.GDPR)) {
            if (request.getBirthDate() != null) {
                manager.setBirthDate(LocalDate.parse(request.getBirthDate()));
            } else {
                manager.setBirthDate(null);
            }
        }
        manager.setAdditionalInfo(request.getAdditionalInformation());
        manager.setStatus(request.getStatus());
        manager.setCustomerDetailId(customerDetailId);
        manager.setTitle(title);
        manager.setRepresentationMethod(representationMethod);
        manager.setCustomerDetailId(customerDetailId);
        tempManagersList.add(manager);
    }

    /**
     * Loops over persisted {@link Manager}s and delete them if not found in the provided request.
     *
     * @param request           {@link List<EditManagerRequest> List&lt;EditManagerRequest&gt;}
     * @param persistedManagers {@link List<Long> List&lt;Long&gt;} IDs of the persisted managers
     * @param exceptions        list of errors which is populated in case of exceptions or validation violations
     */
    private void deleteRemovedManagers(List<EditManagerRequest> request,
                                       List<Long> persistedManagers,
                                       List<String> exceptions) {
        if (!persistedManagers.isEmpty()) {
            List<Long> requestManagerIds = request
                    .stream()
                    .map(EditManagerRequest::getId)
                    .toList();

            for (int i = 0; i < persistedManagers.size(); i++) {
                Long managerId = persistedManagers.get(i);
                if (!requestManagerIds.contains(managerId)) {
                    deleteManager(managerId, exceptions, i);
                }
            }
        }
    }

    /**
     * Deletes a manager if not already deleted
     *
     * @param managerId  ID of the {@link Manager} that should be deleted
     * @param exceptions list of errors which is populated in case of exceptions or validation violations
     */
    private void deleteManager(Long managerId, List<String> exceptions, int index) {
        Optional<Manager> managerOptional = managerRepository.findById(managerId);
        if (managerOptional.isEmpty()) {
            log.error("Manager not found, ID: " + managerId);
            exceptions.add("managers[%s].id-Manager not found, ID: [%s];".formatted(index, managerId));
            return;
        }

        Manager manager = managerOptional.get();

        if (!manager.getStatus().equals(DELETED)) {
            manager.setStatus(DELETED);
            managerRepository.save(manager);
        }
    }
}
