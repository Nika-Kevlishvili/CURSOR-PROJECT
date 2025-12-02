package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractAssistingEmployee;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractExternalIntermediary;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractInternalIntermediary;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.service.ServiceContractAdditionalParametersRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBankingDetails;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractEditRequest;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractAdditionalParametersResponse;
import bg.energo.phoenix.repository.contract.service.ServiceContractAssistingEmployeeRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractExternalIntermediaryRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractInternalIntermediaryRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ExternalIntermediaryRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.activity.ServiceContractActivityService;
import bg.energo.phoenix.service.task.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceContractAdditionalParametersService {
    private final BankRepository bankRepository;
    private final CampaignRepository campaignRepository;
    private final InterestRateRepository interestRateRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final ServiceContractAssistingEmployeeRepository serviceContractAssistingEmployeeRepository;
    private final ServiceContractInternalIntermediaryRepository serviceContractInternalIntermediaryRepository;
    private final ServiceContractExternalIntermediaryRepository serviceContractExternalIntermediaryRepository;
    private final ExternalIntermediaryRepository externalIntermediaryRepository;
    private final PermissionService permissionService;
    private final ServiceContractActivityService serviceContractActivityService;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final TaskService taskService;


    /**
     * Creates additional parameters for the service contract.
     *
     * @param additionalParameters   request containing the additional parameters
     * @param serviceContractDetails the service contract details to be populated
     * @param exceptionMessages      list of exception messages to be returned to the user
     */
    @Transactional
    public void create(ServiceContractAdditionalParametersRequest additionalParameters, ServiceContractDetails serviceContractDetails, List<String> exceptionMessages) {
        log.debug("Creating additional parameters for service contract with request {}", additionalParameters);
        ServiceContractBankingDetails bankingDetails = additionalParameters.getBankingDetails();
        processBankingDetails(serviceContractDetails, exceptionMessages, bankingDetails, List.of(ACTIVE));
        processInterestRate(serviceContractDetails, exceptionMessages, additionalParameters);
        processCampaign(serviceContractDetails, exceptionMessages, additionalParameters, List.of(ACTIVE));
        processEmployeeOnCreate(serviceContractDetails, additionalParameters,exceptionMessages);
    }


    /**
     * Processes employee which is the creator user of the contract during creation and can be modified on update.
     *
     * @param serviceContractDetails the service contract details to be populated
     * @param additionalParametersRequest
     * @param errorMessages          list of exception messages to be returned to the user
     */
    private void processEmployeeOnCreate(ServiceContractDetails serviceContractDetails, ServiceContractAdditionalParametersRequest additionalParametersRequest, List<String> errorMessages) {
        String loggedInUserName = permissionService.getLoggedInUserId();
        if(loggedInUserName==null){
            if(!accountManagerRepository.existsByIdAndStatusIn(additionalParametersRequest.getEmployeeId(),List.of(Status.ACTIVE))){
                errorMessages.add("additionalParameters.employeeId-Wrong employee id provided!;");
            }
            serviceContractDetails.setEmployeeId(additionalParametersRequest.getEmployeeId());
            validateAssistingEmployees(additionalParametersRequest.getEmployeeId(), additionalParametersRequest.getAssistingEmployees(), errorMessages);
            return;
        }
        Optional<AccountManager> employeeOptional = accountManagerRepository.findByUserNameAndStatusIn(loggedInUserName, List.of(Status.ACTIVE));
        if (employeeOptional.isEmpty()) {
            log.error("additionalParameters.employeeId-Unable to find employee with username %s;".formatted(loggedInUserName));
            errorMessages.add("additionalParameters.employeeId-Unable to find employee with username %s;".formatted(loggedInUserName));
        } else {
            AccountManager accountManager = employeeOptional.get();
            serviceContractDetails.setEmployeeId(accountManager.getId());
            validateAssistingEmployees(accountManager.getId(), additionalParametersRequest.getAssistingEmployees(), errorMessages);
        }
    }

    private void validateAssistingEmployees(Long employeeId, List<Long> assistingEmployees, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(assistingEmployees) && assistingEmployees.contains(employeeId)) {
            errorMessages.add("additionalParameters.assistingEmployees-Assisting employee should not match employee;");
        }
    }


    /**
     * Creates sub objects for the service contract.
     *
     * @param serviceContractDetails the service contract details to add the objects to
     * @param exceptionMessages      list of exception messages to be returned to the user
     * @param additionalParameters   request containing the additional parameters
     */
    public void createSubObjectRelations(ServiceContractDetails serviceContractDetails,
                                         List<String> exceptionMessages,
                                         ServiceContractAdditionalParametersRequest additionalParameters) {
        createAssistingEmployees(serviceContractDetails, exceptionMessages, additionalParameters.getAssistingEmployees());
        createInternalIntermediaries(serviceContractDetails, exceptionMessages, additionalParameters.getInternalIntermediaries());
        createExternalIntermediaries(serviceContractDetails, exceptionMessages, additionalParameters.getExternalIntermediaries());
    }


    /**
     * Updates additional parameters for the service contract.
     *
     * @param sourceServiceContractDetails source {@link ServiceContractDetails} to be updated from the request
     * @param targetServiceContractDetails target {@link ServiceContractDetails} to be updated from the request
     * @param exceptionMessages            list of exception messages to be returned to the user
     */
    public void update(ServiceContractDetails sourceServiceContractDetails,
                       ServiceContractDetails targetServiceContractDetails,
                       List<String> exceptionMessages,
                       ServiceContractEditRequest request) {
        ServiceContractAdditionalParametersRequest additionalParameters = request.getAdditionalParameters();
        log.debug("Updating additional parameters for service contract with request {}", additionalParameters);
        processInterestRate(targetServiceContractDetails, exceptionMessages, additionalParameters);
        processEmployeeOnUpdate(targetServiceContractDetails, exceptionMessages, additionalParameters);
        processBankingDetails(
                targetServiceContractDetails,
                exceptionMessages,
                additionalParameters.getBankingDetails(),
                getNomenclatureStatusesOnUpdate(sourceServiceContractDetails.getBankId(), additionalParameters.getBankingDetails().getBankId())
        );
        processCampaign(
                targetServiceContractDetails,
                exceptionMessages,
                additionalParameters,
                getNomenclatureStatusesOnUpdate(sourceServiceContractDetails.getCampaignId(), additionalParameters.getCampaignId())
        );
    }


    /**
     * Updates sub objects or creates new relations depending on the request.
     *
     * @param targetServiceContractDetails the service contract details to be populated
     * @param exceptionMessages            list of exception messages to be returned to the user
     * @param request                      contains all the details for the service contract to update
     * @param additionalParameters         contains all the details for the service contract to update
     */
    public void updateSubObjects(ServiceContractDetails targetServiceContractDetails, List<String> exceptionMessages, ServiceContractEditRequest request, ServiceContractAdditionalParametersRequest additionalParameters) {
        if (request.isSavingAsNewVersion()) {
            createExternalIntermediaries(targetServiceContractDetails, exceptionMessages, additionalParameters.getExternalIntermediaries());
            createInternalIntermediaries(targetServiceContractDetails, exceptionMessages, additionalParameters.getInternalIntermediaries());
            createAssistingEmployees(targetServiceContractDetails, exceptionMessages, additionalParameters.getAssistingEmployees());
        } else {
            updateExternalIntermediaries(additionalParameters.getExternalIntermediaries(), targetServiceContractDetails, exceptionMessages);
            updateInternalIntermediaries(additionalParameters.getInternalIntermediaries(), targetServiceContractDetails, exceptionMessages);
            updateAssistingEmployees(additionalParameters.getAssistingEmployees(), targetServiceContractDetails, exceptionMessages);
        }
    }


    /**
     * Processes employee field on update to be able to change the creator user of the contract.
     *
     * @param targetServiceContractDetails the service contract details to be populated
     * @param errorMessages                list of exception messages to be returned to the user
     * @param additionalParameters         contains all the details for the service contract to update
     */
    private void processEmployeeOnUpdate(ServiceContractDetails targetServiceContractDetails,
                                         List<String> errorMessages,
                                         ServiceContractAdditionalParametersRequest additionalParameters) {
        if (additionalParameters.getEmployeeId() == null) {
            log.error("additionalParameters.employeeId-Employee id is mandatory;");
            errorMessages.add("additionalParameters.employeeId-Employee id is mandatory;");
            return;
        }

        if (!accountManagerRepository.existsByIdAndStatusIn(additionalParameters.getEmployeeId(), List.of(Status.ACTIVE))) {
            log.error("additionalParameters.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(additionalParameters.getEmployeeId(), List.of(Status.ACTIVE)));
            errorMessages.add("additionalParameters.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(additionalParameters.getEmployeeId(), List.of(Status.ACTIVE)));
        } else {
            targetServiceContractDetails.setEmployeeId(additionalParameters.getEmployeeId());
            validateAssistingEmployees(additionalParameters.getEmployeeId(), additionalParameters.getAssistingEmployees(), errorMessages);

        }
    }


    /**
     * Defines the statuses to be used for validation of nomenclature items on update.
     *
     * @param sourceNomenclatureId value of the nomenclature item from source detail before the update
     * @param targetNomenclatureId value of the nomenclature item from update request
     * @return list of statuses to be used for validation
     */
    private List<NomenclatureItemStatus> getNomenclatureStatusesOnUpdate(Long sourceNomenclatureId, Long targetNomenclatureId) {
        if (Objects.equals(sourceNomenclatureId, targetNomenclatureId)) {
            // inactive nomenclature is allowed to be selected/saved only when it's the same as the one from the source detail
            // if both are null, it won't be a problem as the process method will handle it
            return List.of(ACTIVE, INACTIVE);
        } else {
            return List.of(ACTIVE);
        }
    }


    /**
     * Validates and creates external intermediaries for the contract.
     *
     * @param serviceContractDetails  the service contract details to add the objects to
     * @param exceptionMessages       list of exception messages to be returned to the user
     * @param externalIntermediaryIds list of external intermediary ids to be created
     */
    public void createExternalIntermediaries(ServiceContractDetails serviceContractDetails, List<String> exceptionMessages, List<Long> externalIntermediaryIds) {
        if (CollectionUtils.isNotEmpty(externalIntermediaryIds)) {
            List<Long> activeExternalIntermediaryIds = externalIntermediaryRepository.findExistingByIdInAndStatusIn(externalIntermediaryIds, List.of(NomenclatureItemStatus.ACTIVE));

            List<ServiceContractExternalIntermediary> contractExternalIntermediaries = new ArrayList<>();
            for (int i = 0, externalIntermediariesIdsSize = externalIntermediaryIds.size(); i < externalIntermediariesIdsSize; i++) {
                Long externalIntermediaryId = externalIntermediaryIds.get(i);
                if (activeExternalIntermediaryIds.contains(externalIntermediaryId)) {
                    contractExternalIntermediaries.add(new ServiceContractExternalIntermediary(null, externalIntermediaryId, serviceContractDetails.getId(), EntityStatus.ACTIVE));
                } else {
                    exceptionMessages.add("additionalParameters.externalIntermediaries[%s]-External Intermediary with presented id [%s] not found;".formatted(i, externalIntermediaryId));
                }
            }

            if (exceptionMessages.isEmpty()) {
                serviceContractExternalIntermediaryRepository.saveAll(contractExternalIntermediaries);
            }
        }
    }


    /**
     * Validates and creates internal intermediaries for the contract.
     *
     * @param serviceContractDetails  the service contract details to add the objects to
     * @param exceptionMessages       list of exception messages to be returned to the user
     * @param internalIntermediaryIds list of internal intermediary ids to be created
     */
    public void createInternalIntermediaries(ServiceContractDetails serviceContractDetails, List<String> exceptionMessages, List<Long> internalIntermediaryIds) {
        if (CollectionUtils.isNotEmpty(internalIntermediaryIds)) {
            List<Long> activeInternalIntermediaryIds = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), internalIntermediaryIds);

            List<ServiceContractInternalIntermediary> contractInternalIntermediaries = new ArrayList<>();
            for (int i = 0, internalIntermediariesIdsSize = internalIntermediaryIds.size(); i < internalIntermediariesIdsSize; i++) {
                Long internalIntermediaryId = internalIntermediaryIds.get(i);
                if (activeInternalIntermediaryIds.contains(internalIntermediaryId)) {
                    contractInternalIntermediaries.add(new ServiceContractInternalIntermediary(null, internalIntermediaryId, serviceContractDetails.getId(), EntityStatus.ACTIVE));
                } else {
                    exceptionMessages.add("additionalParameters.internalIntermediaries[%s]-Internal Intermediary with presented id [%s] not found;".formatted(i, internalIntermediaryId));
                }
            }

            if (exceptionMessages.isEmpty()) {
                serviceContractInternalIntermediaryRepository.saveAll(contractInternalIntermediaries);
            }
        }
    }


    /**
     * Validates and creates assisting employees for the contract.
     *
     * @param serviceContractDetails the service contract details to add the objects to
     * @param exceptionMessages      list of exception messages to be returned to the user
     * @param assistingEmployeeIds   list of assisting employee ids to be created
     */
    public void createAssistingEmployees(ServiceContractDetails serviceContractDetails, List<String> exceptionMessages, List<Long> assistingEmployeeIds) {
        if (CollectionUtils.isNotEmpty(assistingEmployeeIds)) {
            List<Long> activeAssigningEmployeeIds = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), assistingEmployeeIds);

            List<ServiceContractAssistingEmployee> serviceContractAssigningEmployees = new ArrayList<>();
            for (int i = 0, assistingEmployeeIdsSize = assistingEmployeeIds.size(); i < assistingEmployeeIdsSize; i++) {
                Long assistingEmployeeId = assistingEmployeeIds.get(i);
                if (activeAssigningEmployeeIds.contains(assistingEmployeeId)) {
                    serviceContractAssigningEmployees.add(new ServiceContractAssistingEmployee(null, assistingEmployeeId, serviceContractDetails.getId(), EntityStatus.ACTIVE));
                } else {
                    exceptionMessages.add("additionalParameters.assistingEmployees[%s]-Assisting Employee with presented id [%s] not found;".formatted(i, assistingEmployeeId));
                }
            }

            if (exceptionMessages.isEmpty()) {
                serviceContractAssistingEmployeeRepository.saveAll(serviceContractAssigningEmployees);
            }
        }
    }


    /**
     * Processes campaign for the service contract.
     *
     * @param serviceContractDetails the service contract details to update
     * @param exceptionMessages      list of exception messages to be returned to the user
     * @param additionalParameters   contains all the date for the service contract to update
     * @param statuses               list of statuses to be used for validation
     */
    private void processCampaign(ServiceContractDetails serviceContractDetails,
                                 List<String> exceptionMessages,
                                 ServiceContractAdditionalParametersRequest additionalParameters,
                                 List<NomenclatureItemStatus> statuses) {
        if (additionalParameters.getCampaignId() != null) {
            Optional<Campaign> campaignOptional = campaignRepository.findByIdAndStatusIn(additionalParameters.getCampaignId(), statuses);
            if (campaignOptional.isEmpty()) {
                log.error("additionalParameters.campaignId-Campaign id with presented id [%s] not found;".formatted(additionalParameters.getInterestRateId()));
                exceptionMessages.add("additionalParameters.campaignId-Campaign id with presented id [%s] not found;".formatted(additionalParameters.getInterestRateId()));
            }
        }
        serviceContractDetails.setCampaignId(additionalParameters.getCampaignId());
    }


    /**
     * Processes interest rate for the service contract.
     *
     * @param serviceContractDetails the service contract details to update
     * @param exceptionMessages      list of exception messages to be returned to the user
     * @param additionalParameters   contains all the date for the service contract to update
     */
    private void processInterestRate(ServiceContractDetails serviceContractDetails, List<String> exceptionMessages, ServiceContractAdditionalParametersRequest additionalParameters) {
        Optional<InterestRate> interestRateOptional = interestRateRepository.findByIdAndStatusIn(additionalParameters.getInterestRateId(), List.of(InterestRateStatus.ACTIVE));
        if (interestRateOptional.isEmpty()) {
            log.error("additionalParameters.interestRateId-Interest rate with presented id [%s] not found;".formatted(additionalParameters.getInterestRateId()));
            exceptionMessages.add("additionalParameters.interestRateId-Interest rate with presented id [%s] not found;".formatted(additionalParameters.getInterestRateId()));
        } else {
            serviceContractDetails.setApplicableInterestRate(interestRateOptional.get().getId());
        }
    }


    /**
     * Processes banking details for the service contract.
     *
     * @param serviceContractDetails the service contract details to update
     * @param exceptionMessages      list of exception messages to be returned to the user
     * @param bankingDetails         contains all the date for the service contract to update
     * @param statuses               list of statuses to be used for validation
     */
    private void processBankingDetails(ServiceContractDetails serviceContractDetails,
                                       List<String> exceptionMessages,
                                       ServiceContractBankingDetails bankingDetails,
                                       List<NomenclatureItemStatus> statuses) {
        serviceContractDetails.setDirectDebit(bankingDetails.getDirectDebit());
        serviceContractDetails.setBankId(bankingDetails.getBankId());
        serviceContractDetails.setIban(bankingDetails.getIban());
        if (bankingDetails.getDirectDebit()) {
            if (!bankRepository.existsByIdAndStatusIn(bankingDetails.getBankId(), statuses)) {
                log.error("additionalParameters.bankingDetails.bankId-Bank with presented id [%s] not found;".formatted(bankingDetails.getBankId()));
                exceptionMessages.add("additionalParameters.bankingDetails.bankId-Bank with presented id [%s] not found;".formatted(bankingDetails.getBankId()));
            }
        }
    }


    /**
     * Updates external intermediaries for the service contract.
     *
     * @param externalIntermediaries list of external intermediary ids to be updated
     * @param serviceContractDetails the service contract details to update
     * @param errorMessages          list of exception messages to be returned to the user
     */
    public void updateExternalIntermediaries(List<Long> externalIntermediaries,
                                             ServiceContractDetails serviceContractDetails,
                                             List<String> errorMessages) {
        List<ServiceContractExternalIntermediary> persistedExternalIntermediaries = serviceContractExternalIntermediaryRepository
                .findByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(externalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedExternalIntermediaries)) {
                // user has removed all external intermediaries, should set deleted status to them
                persistedExternalIntermediaries.forEach(contractExternalIntermediary -> contractExternalIntermediary.setStatus(EntityStatus.DELETED));
                serviceContractExternalIntermediaryRepository.saveAll(persistedExternalIntermediaries);
            }
            return;
        }

        List<ServiceContractExternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that external intermediaries are present in request
        if (CollectionUtils.isEmpty(persistedExternalIntermediaries)) {
            // user has added new external intermediaries, should create them
            createExternalIntermediaries(serviceContractDetails, errorMessages, externalIntermediaries);
            return;
        } else {
            // user has modified existing external intermediaries, should update them
            List<Long> persistedExternalIntermediaryIds = persistedExternalIntermediaries
                    .stream()
                    .map(ServiceContractExternalIntermediary::getExternalIntermediaryId)
                    .toList();

            for (int i = 0; i < externalIntermediaries.size(); i++) {
                Long externalIntermediary = externalIntermediaries.get(i);

                if (persistedExternalIntermediaryIds.contains(externalIntermediary)) {
                    // if the external intermediary is already persisted, we validate its presence in ACTIVE and INACTIVE nomenclatures
                    if (!externalIntermediaryRepository.existsByIdAndStatusIn(externalIntermediary, List.of(ACTIVE, INACTIVE))) {
                        log.error("additionalParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE, INACTIVE)));
                        errorMessages.add("additionalParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE, INACTIVE)));
                        continue;
                    }

                    Optional<ServiceContractExternalIntermediary> persistedExternalIntermediaryOptional = persistedExternalIntermediaries
                            .stream()
                            .filter(contractExternalIntermediary -> contractExternalIntermediary.getExternalIntermediaryId().equals(externalIntermediary))
                            .findFirst();
                    if (persistedExternalIntermediaryOptional.isEmpty()) {
                        log.error("additionalParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                        errorMessages.add("additionalParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                    } else {
                        ServiceContractExternalIntermediary persistedExternalIntermediary = persistedExternalIntermediaryOptional.get();
                        persistedExternalIntermediary.setExternalIntermediaryId(externalIntermediary);
                        tempList.add(persistedExternalIntermediary);
                    }
                } else {
                    // if the external intermediary is not persisted, we validate its presence in ACTIVE nomenclature
                    if (!externalIntermediaryRepository.existsByIdAndStatusIn(externalIntermediary, List.of(ACTIVE))) {
                        log.error("additionalParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE)));
                        errorMessages.add("additionalParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE)));
                        continue;
                    }

                    ServiceContractExternalIntermediary extInt = new ServiceContractExternalIntermediary();
                    extInt.setExternalIntermediaryId(externalIntermediary);
                    extInt.setContractDetailId(serviceContractDetails.getId());
                    extInt.setStatus(EntityStatus.ACTIVE);
                    tempList.add(extInt);
                }
            }

            // if the external intermediary is not present in the request, we set its status to DELETED
            for (ServiceContractExternalIntermediary externalIntermediary : persistedExternalIntermediaries) {
                if (!externalIntermediaries.contains(externalIntermediary.getExternalIntermediaryId())) {
                    externalIntermediary.setStatus(EntityStatus.DELETED);
                    tempList.add(externalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceContractExternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Updates assisting employees for the service contract.
     *
     * @param assistingEmployees     list of assisting employee ids to be updated
     * @param serviceContractDetails the service contract details to update
     * @param errorMessages          list of exception messages to be returned to the user
     */
    public void updateAssistingEmployees(List<Long> assistingEmployees,
                                         ServiceContractDetails serviceContractDetails,
                                         List<String> errorMessages) {
        List<ServiceContractAssistingEmployee> persistedAssistingEmployees = serviceContractAssistingEmployeeRepository
                .findByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(assistingEmployees)) {
            if (CollectionUtils.isNotEmpty(persistedAssistingEmployees)) {
                // user has removed all assisting employees, should set deleted status to them
                persistedAssistingEmployees.forEach(assistingEmployee -> assistingEmployee.setStatus(EntityStatus.DELETED));
                serviceContractAssistingEmployeeRepository.saveAll(persistedAssistingEmployees);
            }
            return;
        }

        List<ServiceContractAssistingEmployee> tempList = new ArrayList<>();

        // at this moment we already know that assisting employees list is not empty
        if (CollectionUtils.isEmpty(persistedAssistingEmployees)) {
            // user has added new assisting employees, should create them
            createAssistingEmployees(serviceContractDetails, errorMessages, assistingEmployees);
            return;
        } else {
            // user has modified (added/edited) assisting employees, should update them
            List<Long> persistedAssistingEmployeeIds = persistedAssistingEmployees
                    .stream()
                    .map(ServiceContractAssistingEmployee::getAssistingEmployeeId)
                    .toList();

            List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), assistingEmployees);

            for (int i = 0; i < assistingEmployees.size(); i++) {
                Long assistingEmployee = assistingEmployees.get(i);
                if (!systemUsers.contains(assistingEmployee)) {
                    log.error("additionalParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                    errorMessages.add("additionalParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                    continue;
                }

                if (!persistedAssistingEmployeeIds.contains(assistingEmployee)) {
                    ServiceContractAssistingEmployee assistingEmp = new ServiceContractAssistingEmployee();
                    assistingEmp.setAssistingEmployeeId(assistingEmployee);
                    assistingEmp.setStatus(EntityStatus.ACTIVE);
                    assistingEmp.setContractDetailId(serviceContractDetails.getId());
                    tempList.add(assistingEmp);
                } else {
                    Optional<ServiceContractAssistingEmployee> persistedAssistingEmployeeOptional = persistedAssistingEmployees
                            .stream()
                            .filter(assistingEmployee1 -> assistingEmployee1.getAssistingEmployeeId().equals(assistingEmployee))
                            .findFirst();

                    if (persistedAssistingEmployeeOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                    } else {
                        ServiceContractAssistingEmployee persistedAssistingEmployee = persistedAssistingEmployeeOptional.get();
                        persistedAssistingEmployee.setAssistingEmployeeId(assistingEmployee);
                        tempList.add(persistedAssistingEmployee);
                    }
                }
            }

            // user has removed some assisting employees, should set deleted status to them
            for (ServiceContractAssistingEmployee assistingEmployee : persistedAssistingEmployees) {
                if (!assistingEmployees.contains(assistingEmployee.getAssistingEmployeeId())) {
                    assistingEmployee.setStatus(EntityStatus.DELETED);
                    tempList.add(assistingEmployee);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceContractAssistingEmployeeRepository.saveAll(tempList);
        }
    }


    /**
     * Updates internal intermediaries for the service contract.
     *
     * @param internalIntermediaries list of internal intermediary ids to be updated
     * @param serviceContractDetails the service contract details to update
     * @param errorMessages          list of exception messages to be returned to the user
     */
    public void updateInternalIntermediaries(List<Long> internalIntermediaries,
                                             ServiceContractDetails serviceContractDetails,
                                             List<String> errorMessages) {
        List<ServiceContractInternalIntermediary> persistedInternalIntermediaries = serviceContractInternalIntermediaryRepository
                .findByContractDetailIdAndStatusIn(serviceContractDetails.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(internalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedInternalIntermediaries)) {
                // user has removed all internal intermediaries, should set deleted status to them
                persistedInternalIntermediaries.forEach(contractInternalIntermediary -> contractInternalIntermediary.setStatus(EntityStatus.DELETED));
                serviceContractInternalIntermediaryRepository.saveAll(persistedInternalIntermediaries);
            }
            return;
        }

        List<ServiceContractInternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that internalIntermediaries list is not empty
        if (CollectionUtils.isEmpty(persistedInternalIntermediaries)) {
            // user has added new internal intermediaries, should create them
            createInternalIntermediaries(serviceContractDetails, errorMessages, internalIntermediaries);
            return;
        } else {
            // user has modified (added/edited) internal intermediaries, should update them
            List<Long> persistedInternalIntermediaryIds = persistedInternalIntermediaries
                    .stream()
                    .map(ServiceContractInternalIntermediary::getInternalIntermediaryId)
                    .toList();

            List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), internalIntermediaries);

            for (int i = 0; i < internalIntermediaries.size(); i++) {
                Long internalIntermediary = internalIntermediaries.get(i);
                if (!systemUsers.contains(internalIntermediary)) {
                    log.error("additionalParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                    errorMessages.add("additionalParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                    continue;
                }

                if (!persistedInternalIntermediaryIds.contains(internalIntermediary)) {
                    ServiceContractInternalIntermediary internalInt = new ServiceContractInternalIntermediary();
                    internalInt.setInternalIntermediaryId(internalIntermediary);
                    internalInt.setStatus(EntityStatus.ACTIVE);
                    internalInt.setContractDetailId(serviceContractDetails.getId());
                    tempList.add(internalInt);
                } else {
                    Optional<ServiceContractInternalIntermediary> persistedInternalIntermediaryOptional = persistedInternalIntermediaries
                            .stream()
                            .filter(contractInternalIntermediary -> contractInternalIntermediary.getInternalIntermediaryId().equals(internalIntermediary))
                            .findFirst();
                    if (persistedInternalIntermediaryOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                    } else {
                        ServiceContractInternalIntermediary contractInternalIntermediary = persistedInternalIntermediaryOptional.get();
                        contractInternalIntermediary.setInternalIntermediaryId(internalIntermediary);
                        tempList.add(contractInternalIntermediary);
                    }
                }
            }

            // user has removed some internal intermediaries, should set deleted status to them
            for (ServiceContractInternalIntermediary internalIntermediary : persistedInternalIntermediaries) {
                if (!internalIntermediaries.contains(internalIntermediary.getInternalIntermediaryId())) {
                    internalIntermediary.setStatus(EntityStatus.DELETED);
                    tempList.add(internalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceContractInternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Gets additional parameters for the service contract preview.
     *
     * @param details the service contract details to get the parameters for
     * @return {@link ServiceContractAdditionalParametersResponse} containing the additional parameters
     */
    public ServiceContractAdditionalParametersResponse getAdditionalParameters(ServiceContractDetails details) {
        log.debug("Getting additional parameters for contract with ID %s".formatted(details.getId()));

        ServiceContractAdditionalParametersResponse response = serviceContractDetailsRepository
                .getAdditionalParametersByContractDetailId(details.getId());

        // NOTE: id field in the following objects represents the account manager id (in internal intermediaries and assisting employees)
        // and external intermediary id (in external intermediaries), and not a db record id.

        response.setAssistingEmployees(
                serviceContractAssistingEmployeeRepository
                        .getShortResponseByContractDetailIdAndStatusIn(
                                details.getId(),
                                List.of(EntityStatus.ACTIVE)
                        )
        );

        response.setInternalIntermediaries(
                serviceContractInternalIntermediaryRepository
                        .getShortResponseByContractDetailIdAndStatusIn(
                                details.getId(),
                                List.of(EntityStatus.ACTIVE)
                        )
        );

        response.setExternalIntermediaries(
                serviceContractExternalIntermediaryRepository
                        .getShortResponseByContractDetailIdAndStatusIn(
                                details.getId(),
                                List.of(EntityStatus.ACTIVE)
                        )
        );

        response.setActivities(serviceContractActivityService.getActivitiesByConnectedObjectId(details.getContractId()));
        response.setTasks(taskService.getTasksByServiceContractId(details.getContractId()));

        return response;
    }
}
