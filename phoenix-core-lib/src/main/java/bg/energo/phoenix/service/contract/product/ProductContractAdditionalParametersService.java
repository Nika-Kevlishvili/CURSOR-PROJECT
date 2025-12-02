package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ContractAssistingEmployee;
import bg.energo.phoenix.model.entity.contract.product.ContractExternalIntermediary;
import bg.energo.phoenix.model.entity.contract.product.ContractInternalIntermediary;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.product.ProductContractCreateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractPointOfDeliveryRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractAdditionalParametersRequest;
import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractBankingDetails;
import bg.energo.phoenix.model.response.contract.productContract.AdditionalParametersResponse;
import bg.energo.phoenix.repository.contract.product.ContractAssistingEmployeeRepository;
import bg.energo.phoenix.repository.contract.product.ContractExternalIntermediaryRepository;
import bg.energo.phoenix.repository.contract.product.ContractInternalIntermediaryRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ExternalIntermediaryRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.activity.ProductContractActivityService;
import bg.energo.phoenix.service.riskList.RiskListService;
import bg.energo.phoenix.service.riskList.model.RiskListBasicInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListDecision;
import bg.energo.phoenix.service.riskList.model.RiskListRequest;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBListUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractAdditionalParametersService {

    private final RiskListService riskListService;
    private final CustomerRepository customerRepository;
    private final BankRepository bankRepository;
    private final CampaignRepository campaignRepository;
    private final ContractInternalIntermediaryRepository contractInternalIntermediaryRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final ExternalIntermediaryRepository externalIntermediaryRepository;
    private final ContractExternalIntermediaryRepository contractExternalIntermediaryRepository;
    private final ContractAssistingEmployeeRepository contractAssistingEmployeeRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final InterestRateRepository interestRateRepository;
    private final PointOfDeliveryDetailsRepository podDetailsRepository;
    private final ProductContractActivityService productContractActivityService;
    private final TaskService taskService;
    private final PermissionService permissionService;


    /**
     * Processes additional parameters tab details for product contract
     *
     * @param request                {@link ProductContractCreateRequest} containing all the details for product contract creation
     * @param productContractDetails {@link ProductContractDetails} to be updated with the information from the request
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    @Transactional
    public void create(ProductContractCreateRequest request,
                       ProductContractDetails productContractDetails,
                       List<String> errorMessages) {
        ProductContractAdditionalParametersRequest additionalParameters = request.getAdditionalParameters();
        log.debug("Creating additional parameters for contract with request: {};", additionalParameters);

        // TODO: 17.07.23 deal number field details will be specified in the following bundles. At the moment it's just an optional field.
        productContractDetails.setDealNumber(additionalParameters.getDealNumber());

        processEmployeeOnCreate(productContractDetails, additionalParameters, errorMessages);
        processEstimatedTotalConsumption(request.getProductContractPointOfDeliveries().stream().map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId).toList(), additionalParameters.getEstimatedTotalConsumptionUnderContractKwh(), productContractDetails, errorMessages);
        processBankingDetails(additionalParameters.getBankingDetails(), productContractDetails, List.of(ACTIVE), errorMessages);
        processCampaign(productContractDetails, additionalParameters.getCampaignId(), List.of(ACTIVE), errorMessages);
        processApplicableInterestRate(productContractDetails, additionalParameters.getInterestRateId(), List.of(InterestRateStatus.ACTIVE), errorMessages);
        validateCustomerInRiskListAPI(
                request.getBasicParameters().getCustomerId(),
                request.getBasicParameters().getCustomerVersionId(),
                additionalParameters.getEstimatedTotalConsumptionUnderContractKwh(),
                productContractDetails,
                errorMessages
        );
    }


    /**
     * Processes employee which is the creator user of the contract during creation and can be modified on update
     *
     * @param productContractDetails contract details object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void processEmployeeOnCreate(ProductContractDetails productContractDetails, ProductContractAdditionalParametersRequest additionalParametersRequest, List<String> errorMessages) {
        String loggedInUserName = permissionService.getLoggedInUserId();
        //Checks if process is mass import, if yes sets user from provided request
        if (loggedInUserName == null) {
            if (!accountManagerRepository.existsByIdAndStatusIn(additionalParametersRequest.getEmployeeId(), List.of(Status.ACTIVE))) {
                errorMessages.add("additionalParameters.employeeId-Wrong employee id provided!;");
            }
            productContractDetails.setEmployeeId(additionalParametersRequest.getEmployeeId());
            validateAssistingEmployees(additionalParametersRequest.getEmployeeId(), additionalParametersRequest.getAssistingEmployees(), errorMessages);
            return;
        }
        Optional<AccountManager> employeeOptional = accountManagerRepository.findByUserNameAndStatusIn(loggedInUserName, List.of(Status.ACTIVE));
        if (employeeOptional.isEmpty()) {
            log.error("additionalParameters.employeeId-Unable to find employee with username %s;".formatted(loggedInUserName));
            errorMessages.add("additionalParameters.employeeId-Unable to find employee with username %s;".formatted(loggedInUserName));
        } else {
            AccountManager accountManager = employeeOptional.get();
            productContractDetails.setEmployeeId(accountManager.getId());
            validateAssistingEmployees(accountManager.getId(), additionalParametersRequest.getAssistingEmployees(), errorMessages);
        }
    }

    private void validateAssistingEmployees(Long employeeId, List<Long> assistingEmployees, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(assistingEmployees) && assistingEmployees.contains(employeeId)) {
            errorMessages.add("additionalParameters.assistingEmployees-Assisting employee should not match employee;");
        }
    }


    /**
     * Processes additional parameters tab details for product contract
     *
     * @param request                      {@link ProductContractUpdateRequest} containing all the details for product contract update
     * @param sourceProductContractDetails source {@link ProductContractDetails} to be updated with the information from the request
     * @param targetProductContractDetails target {@link ProductContractDetails} to be updated with the information from the request
     * @param errorMessages                list of error messages to be populated in case of validation errors
     */
    @Transactional
    public void update(ProductContractUpdateRequest request,
                       ProductContractDetails sourceProductContractDetails,
                       ProductContractDetails targetProductContractDetails,
                       List<String> errorMessages) {
        ProductContractAdditionalParametersRequest additionalParameters = request.getAdditionalParameters();
        log.debug("Updating additional parameters for contract with request: {};", additionalParameters);
        // TODO: 9/18/23 deal number field details will be specified in the following bundles. At the moment it's just an optional field.
        if (request.getBasicParameters().getVersionStatus() == ProductContractVersionStatus.SIGNED) {
            targetProductContractDetails.setDealNumber(additionalParameters.getDealNumber());
        } else {
            targetProductContractDetails.setDealNumber(null);
        }

        processEmployeeOnUpdate(targetProductContractDetails, errorMessages, additionalParameters);
        List<Long> podIds =
                request
                        .getPodRequests()
                        .stream()
                        .flatMap(x ->
                                x.getProductContractPointOfDeliveries()
                                        .stream()
                                        .map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId)
                        )
                        .toList();
        processEstimatedTotalConsumption(podIds, additionalParameters.getEstimatedTotalConsumptionUnderContractKwh(), targetProductContractDetails, errorMessages);
        processApplicableInterestRate(targetProductContractDetails, additionalParameters.getInterestRateId(), List.of(InterestRateStatus.ACTIVE), errorMessages);

        processBankingDetails(
                additionalParameters.getBankingDetails(),
                targetProductContractDetails,
                getNomenclatureStatusesOnUpdate(sourceProductContractDetails.getBankId(), additionalParameters.getBankingDetails().getBankId()),
                errorMessages
        );

        processCampaign(
                targetProductContractDetails,
                additionalParameters.getCampaignId(),
                getNomenclatureStatusesOnUpdate(sourceProductContractDetails.getCampaignId(), additionalParameters.getCampaignId()),
                errorMessages
        );

        validateCustomerInRiskListAPI(
                request.getBasicParameters().getCustomerId(),
                request.getBasicParameters().getCustomerVersionId(),
                additionalParameters.getEstimatedTotalConsumptionUnderContractKwh(),
                targetProductContractDetails,
                errorMessages
        );
    }


    /**
     * Updates intermediary and assisting sub objects if the same version is edited
     * or creates a new version of them if the contract is saved as a new version.
     *
     * @param request                      {@link ProductContractUpdateRequest} containing all the details for product contract update
     * @param sourceProductContractDetails source {@link ProductContractDetails} to be updated with the information from the request
     * @param targetProductContractDetails target {@link ProductContractDetails} to be updated with the information from the request
     * @param errorMessages                list of error messages to be populated in case of validation errors
     */
    protected void updateAdditionalParametersSubObjects(ProductContractUpdateRequest request, ProductContractDetails sourceProductContractDetails, ProductContractDetails targetProductContractDetails, List<String> errorMessages) {
        if (request.isSavingAsNewVersion()) {
            createInternalIntermediaries(request.getAdditionalParameters().getInternalIntermediaries(), targetProductContractDetails, errorMessages);
            createExternalIntermediaries(request.getAdditionalParameters().getExternalIntermediaries(), targetProductContractDetails, errorMessages);
            createAssistingEmployees(request.getAdditionalParameters().getAssistingEmployees(), targetProductContractDetails, errorMessages);
        } else {
            updateInternalIntermediaries(request.getAdditionalParameters().getInternalIntermediaries(), sourceProductContractDetails, errorMessages);
            updateExternalIntermediaries(request.getAdditionalParameters().getExternalIntermediaries(), sourceProductContractDetails, errorMessages);
            updateAssistingEmployees(request.getAdditionalParameters().getAssistingEmployees(), sourceProductContractDetails, errorMessages);
        }
    }


    /**
     * Processes employee field on update to be able to change the creator user of the contract.
     *
     * @param targetProductContractDetails target {@link ProductContractDetails} to be updated with the information from the request
     * @param errorMessages                list of error messages to be populated in case of validation errors
     * @param additionalParameters         {@link ProductContractAdditionalParametersRequest} containing all the details for product contract update
     */
    private void processEmployeeOnUpdate(ProductContractDetails targetProductContractDetails, List<String> errorMessages, ProductContractAdditionalParametersRequest additionalParameters) {
        if (additionalParameters.getEmployeeId() == null) {
            log.error("additionalParameters.employeeId-Value is mandatory;");
            errorMessages.add("additionalParameters.employeeId-Value is mandatory;");
            return;
        }

        if (!accountManagerRepository.existsByIdAndStatusIn(additionalParameters.getEmployeeId(), List.of(Status.ACTIVE))) {
            log.error("additionalParameters.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(additionalParameters.getEmployeeId(), List.of(Status.ACTIVE)));
            errorMessages.add("additionalParameters.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(additionalParameters.getEmployeeId(), List.of(Status.ACTIVE)));
        } else {
            targetProductContractDetails.setEmployeeId(additionalParameters.getEmployeeId());
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
     * Validate estimated total consumption under contract value according to the selected PODs
     *
     * @param podDetailIds                              list of selected PODs that are optional
     * @param estimatedTotalConsumptionUnderContractKwh value of estimated total consumption under contract
     * @param productContractDetails                    {@link ProductContractDetails} to be updated with the information from the request
     * @param errorMessages                             list of error messages to be populated in case of validation errors
     */
    private void processEstimatedTotalConsumption(List<Long> podDetailIds,
                                                  BigDecimal estimatedTotalConsumptionUnderContractKwh,
                                                  ProductContractDetails productContractDetails,
                                                  List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(podDetailIds)) {
            List<Integer> podConsumptions = podDetailsRepository.findEstimatedMonthlyAvgConsumptionByIdIn(podDetailIds);
            int totalConsumption = podConsumptions.stream().mapToInt(Integer::intValue).sum();
            BigDecimal summedConsumption = BigDecimal.valueOf(totalConsumption * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED);
            if (estimatedTotalConsumptionUnderContractKwh.compareTo(summedConsumption) != 0) {
                log.error("additionalParameters.estimatedTotalConsumptionUnderContractKwh-Value should match summed up value of estimated monthly average consumption of the selected PODs;");
                errorMessages.add("additionalParameters.estimatedTotalConsumptionUnderContractKwh-Value should match summed up value of estimated monthly average consumption of the selected PODs;");
            }
        }
        productContractDetails.setEstimatedTotalConsumptionUnderContractKwh(estimatedTotalConsumptionUnderContractKwh);
    }


    /**
     * Creates sub objects related to additional parameters of the contract and updates the contract details object
     *
     * @param request                {@link ProductContractCreateRequest} containing all the details for product contract sub objects creation
     * @param productContractDetails {@link ProductContractDetails} to be updated with the information from the request
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    public void createAdditionalParametersSubObjects(ProductContractCreateRequest request,
                                                     ProductContractDetails productContractDetails,
                                                     List<String> errorMessages) {
        createInternalIntermediaries(request.getAdditionalParameters().getInternalIntermediaries(), productContractDetails, errorMessages);
        createExternalIntermediaries(request.getAdditionalParameters().getExternalIntermediaries(), productContractDetails, errorMessages);
        createAssistingEmployees(request.getAdditionalParameters().getAssistingEmployees(), productContractDetails, errorMessages);
    }


    /**
     * Validates contract banking details and updates the contract details object
     *
     * @param bankingDetails         banking details from request
     * @param productContractDetails contract details object
     * @param statuses               statuses to be used for validation
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void processBankingDetails(ProductContractBankingDetails bankingDetails,
                                       ProductContractDetails productContractDetails,
                                       List<NomenclatureItemStatus> statuses,
                                       List<String> errorMessages) {
        productContractDetails.setDirectDebit(bankingDetails.getDirectDebit());
        if (bankingDetails.getBankId() != null) {
            if (!bankRepository.existsByIdAndStatusIn(bankingDetails.getBankId(), statuses)) {
                log.error("additionalParameters.bankingDetails.bankId-Unable to find bank with ID %s in statuses %s;"
                        .formatted(bankingDetails.getBankId(), statuses));
                errorMessages.add("additionalParameters.bankingDetails.bankId-Unable to find bank with ID %s in statuses %s;"
                        .formatted(bankingDetails.getBankId(), statuses));
            }
        }
        productContractDetails.setBankId(bankingDetails.getBankId());
        productContractDetails.setIban(bankingDetails.getIban());
    }


    /**
     * Validates campaign nomenclature and sets it to the contract details object
     *
     * @param productContractDetails contract details object
     * @param campaignId             campaign ID from request
     * @param statuses               statuses to be used for validation
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void processCampaign(ProductContractDetails productContractDetails,
                                 Long campaignId,
                                 List<NomenclatureItemStatus> statuses,
                                 List<String> errorMessages) {
        if (campaignId != null && (!campaignRepository.existsByIdAndStatusIn(campaignId, statuses))) {
            log.error("additionalParameters.campaignId-Unable to find campaign with ID %s in statuses %s;"
                    .formatted(campaignId, statuses));
            errorMessages.add("additionalParameters.campaignId-Unable to find campaign with ID %s in statuses %s;"
                    .formatted(campaignId, statuses));

        }
        productContractDetails.setCampaignId(campaignId);
    }


    /**
     * Validates applicable interest rate and sets it to the contract details object
     *
     * @param productContractDetails contract details object
     * @param interestRateId         interest rate ID from request
     * @param statuses               statuses to be used for validation
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void processApplicableInterestRate(ProductContractDetails productContractDetails,
                                               Long interestRateId,
                                               List<InterestRateStatus> statuses,
                                               List<String> errorMessages) {
        if (!interestRateRepository.existsByIdAndStatusIn(interestRateId, statuses)) {
            log.error("additionalParameters.interestRateId-Unable to find interest rate with ID %s in statuses %s;"
                    .formatted(interestRateId, statuses));
            errorMessages.add("additionalParameters.interestRateId-Unable to find interest rate with ID %s in statuses %s;"
                    .formatted(interestRateId, statuses));
        }
        productContractDetails.setApplicableInterestRate(interestRateId);
    }


    /**
     * Evaluates customer risk in Risk List API to decide whether to allow the contract to be concluded or not.
     *
     * @param customerId                                customer ID from request
     * @param customerVersionId                         customer version from request
     * @param estimatedTotalConsumptionUnderContractKwh estimated total consumption under contract value from request
     * @param productContractDetails                    contract details object to be updated
     * @param errorMessages                             list of error messages to be populated in case of validation errors
     */
    private void validateCustomerInRiskListAPI(Long customerId,
                                               Long customerVersionId,
                                               BigDecimal estimatedTotalConsumptionUnderContractKwh,
                                               ProductContractDetails productContractDetails,
                                               List<String> errorMessages) {
        estimatedTotalConsumptionUnderContractKwh = estimatedTotalConsumptionUnderContractKwh.setScale(3, RoundingMode.HALF_UP);
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isEmpty()) {
            log.error("additionalParameters.riskAssessment-Unable to evaluate risk as active customer with ID %s not found;"
                    .formatted(customerId));
            errorMessages.add("additionalParameters.riskAssessment-Unable to evaluate risk as active customer with ID %s not found;"
                    .formatted(customerId));
            return;
        }

        RiskListRequest riskListRequest = new RiskListRequest(
                customerOptional.get().getIdentifier(),
                customerVersionId,
                estimatedTotalConsumptionUnderContractKwh
        );

        RiskListBasicInfoResponse riskListBasicInfoResponse = riskListService.evaluateBasicCustomerRisk(riskListRequest);

        if (riskListBasicInfoResponse.getDecision() == null) {
            log.error("additionalParameters.riskAssessment-Unable to evaluate risk for consumption %s;".formatted(estimatedTotalConsumptionUnderContractKwh));
            errorMessages.add("additionalParameters.riskAssessment-Unable to evaluate risk for consumption %s;".formatted(estimatedTotalConsumptionUnderContractKwh));
            return;
        }

        // value returned from the API on "save" button is having priority over the one [if] provided in the request
        if (riskListBasicInfoResponse.getDecision().equals(RiskListDecision.PERMIT)) {
            // only "PERMITTED" risk list decisions are allowed to proceed
            productContractDetails.setRiskAssessment(riskListBasicInfoResponse.getDecision().getValue());
            productContractDetails.setRiskAssessmentAdditionalCondition(
                    riskListBasicInfoResponse.getRecommendations().isEmpty() ? null : String.join(";", riskListBasicInfoResponse.getRecommendations())
            );
        } else {
            log.error("additionalParameters.riskAssessment-Not allowed to conclude a contract with the customer with UIC/PN %s;"
                    .formatted(customerOptional.get().getIdentifier()));
            // in case of a deny, frontend needs the error message to be populated by the decision and recommendations
            // (hence, the custom prefix indicators are needed)
            errorMessages.add("%s-Can’t conclude a contract because of risk assessment restriction;"
                    .formatted(EPBFinalFields.RISK_LIST_DECISION_INDICATOR));
            if (CollectionUtils.isNotEmpty(riskListBasicInfoResponse.getRecommendations())) {
                errorMessages.add("%s-%s".formatted(
                        EPBFinalFields.RISK_LIST_RECOMMENDATIONS_INDICATOR,
                        String.join(";", riskListBasicInfoResponse.getRecommendations()))
                );
            }
        }
    }


    /**
     * Validates and creates internal intermediaries for the contract.
     *
     * @param internalIntermediaries list of internal intermediary IDs
     * @param productContractDetails contract details object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void createInternalIntermediaries(List<Long> internalIntermediaries,
                                              ProductContractDetails productContractDetails,
                                              List<String> errorMessages) {
        if (CollectionUtils.isEmpty(internalIntermediaries)) {
            // adding internal intermediaries is optional
            return;
        }

        String duplicationValidation = EPBListUtils.validateDuplicateValuesByIndexes(internalIntermediaries, "additionalParameters.internalIntermediaries");
        if (StringUtils.isNotEmpty(duplicationValidation)) {
            errorMessages.add(duplicationValidation);
            return;
        }

        List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), internalIntermediaries);

        List<ContractInternalIntermediary> tempList = new ArrayList<>();
        for (int i = 0; i < internalIntermediaries.size(); i++) {
            Long internalIntermediary = internalIntermediaries.get(i);
            if (!systemUsers.contains(internalIntermediary)) {
                log.error("additionalParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                errorMessages.add("additionalParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                continue;
            }

            createInternalIntermediary(productContractDetails, tempList, internalIntermediary);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            contractInternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Creates a single internal intermediary for the contract.
     *
     * @param productContractDetails contract details object to be updated
     * @param tempList               list of internal intermediaries to be saved
     */
    private void createInternalIntermediary(ProductContractDetails productContractDetails,
                                            List<ContractInternalIntermediary> tempList,
                                            Long internalIntermediary) {
        ContractInternalIntermediary intermediary = new ContractInternalIntermediary();
        intermediary.setStatus(ContractSubObjectStatus.ACTIVE);
        intermediary.setAccountManagerId(internalIntermediary);
        intermediary.setContractDetailId(productContractDetails.getId());
        tempList.add(intermediary);
    }


    /**
     * Validates and updates internal intermediaries for the contract.
     *
     * @param internalIntermediaries list of internal intermediary IDs
     * @param productContractDetails contract details object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void updateInternalIntermediaries(List<Long> internalIntermediaries,
                                              ProductContractDetails productContractDetails,
                                              List<String> errorMessages) {
        List<ContractInternalIntermediary> persistedInternalIntermediaries = contractInternalIntermediaryRepository
                .findByContractDetailIdAndStatusIn(productContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));

        if (CollectionUtils.isEmpty(internalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedInternalIntermediaries)) {
                // user has removed all internal intermediaries, should set deleted status to them
                persistedInternalIntermediaries.forEach(contractInternalIntermediary -> contractInternalIntermediary.setStatus(ContractSubObjectStatus.DELETED));
                contractInternalIntermediaryRepository.saveAll(persistedInternalIntermediaries);
            }
            return;
        }

        List<ContractInternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that internalIntermediaries list is not empty
        if (CollectionUtils.isEmpty(persistedInternalIntermediaries)) {
            // user has added new internal intermediaries, should create them
            createInternalIntermediaries(internalIntermediaries, productContractDetails, errorMessages);
            return;
        } else {
            // user has modified (added/edited) internal intermediaries, should update them
            List<Long> persistedInternalIntermediaryIds = persistedInternalIntermediaries
                    .stream()
                    .map(ContractInternalIntermediary::getAccountManagerId)
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
                    createInternalIntermediary(productContractDetails, tempList, internalIntermediary);
                } else {
                    Optional<ContractInternalIntermediary> persistedInternalIntermediaryOptional = persistedInternalIntermediaries
                            .stream()
                            .filter(contractInternalIntermediary -> contractInternalIntermediary.getAccountManagerId().equals(internalIntermediary))
                            .findFirst();
                    if (persistedInternalIntermediaryOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                    } else {
                        ContractInternalIntermediary contractInternalIntermediary = persistedInternalIntermediaryOptional.get();
                        contractInternalIntermediary.setAccountManagerId(internalIntermediary);
                        tempList.add(contractInternalIntermediary);
                    }
                }
            }

            // user has removed some internal intermediaries, should set deleted status to them
            for (ContractInternalIntermediary internalIntermediary : persistedInternalIntermediaries) {
                if (!internalIntermediaries.contains(internalIntermediary.getAccountManagerId())) {
                    internalIntermediary.setStatus(ContractSubObjectStatus.DELETED);
                    tempList.add(internalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            contractInternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Validates and creates external intermediaries for the contract.
     *
     * @param externalIntermediaries list of external intermediary IDs
     * @param productContractDetails contract details object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void createExternalIntermediaries(List<Long> externalIntermediaries,
                                              ProductContractDetails productContractDetails,
                                              List<String> errorMessages) {
        if (CollectionUtils.isEmpty(externalIntermediaries)) {
            // adding external intermediaries is not mandatory
            return;
        }

        String duplicationValidation = EPBListUtils.validateDuplicateValuesByIndexes(externalIntermediaries, "additionalParameters.externalIntermediaries");
        if (StringUtils.isNotEmpty(duplicationValidation)) {
            errorMessages.add(duplicationValidation);
            return;
        }

        List<ContractExternalIntermediary> tempList = new ArrayList<>();

        for (int i = 0; i < externalIntermediaries.size(); i++) {
            Long externalIntermediary = externalIntermediaries.get(i);
            if (!externalIntermediaryRepository.existsByIdAndStatusIn(externalIntermediary, List.of(ACTIVE))) {
                log.error("additionalParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                        .formatted(i, externalIntermediary, List.of(ACTIVE)));
                errorMessages.add("additionalParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                        .formatted(i, externalIntermediary, List.of(ACTIVE)));
                continue;
            }

            createExternalIntermediary(productContractDetails, tempList, externalIntermediary);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            contractExternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Creates external intermediary object and adds it to the list.
     *
     * @param productContractDetails contract details to be updated
     * @param tempList               list of external intermediaries to be updated
     * @param externalIntermediary   external intermediary ID
     */
    private static void createExternalIntermediary(ProductContractDetails productContractDetails,
                                                   List<ContractExternalIntermediary> tempList,
                                                   Long externalIntermediary) {
        ContractExternalIntermediary intermediary = new ContractExternalIntermediary();
        intermediary.setStatus(ContractSubObjectStatus.ACTIVE);
        intermediary.setExternalIntermediaryId(externalIntermediary);
        intermediary.setContractDetailId(productContractDetails.getId());
        tempList.add(intermediary);
    }


    /**
     * Validates and updates external intermediaries for the contract.
     *
     * @param externalIntermediaries list of external intermediary IDs
     * @param productContractDetails contract details object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void updateExternalIntermediaries(List<Long> externalIntermediaries,
                                              ProductContractDetails productContractDetails,
                                              List<String> errorMessages) {
        List<ContractExternalIntermediary> persistedExternalIntermediaries = contractExternalIntermediaryRepository
                .findByContractDetailIdAndStatusIn(productContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));

        if (CollectionUtils.isEmpty(externalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedExternalIntermediaries)) {
                // user has removed all external intermediaries, should set deleted status to them
                persistedExternalIntermediaries.forEach(contractExternalIntermediary -> contractExternalIntermediary.setStatus(ContractSubObjectStatus.DELETED));
                contractExternalIntermediaryRepository.saveAll(persistedExternalIntermediaries);
            }
            return;
        }

        List<ContractExternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that external intermediaries are present in request
        if (CollectionUtils.isEmpty(persistedExternalIntermediaries)) {
            // user has added new external intermediaries, should create them
            createExternalIntermediaries(externalIntermediaries, productContractDetails, errorMessages);
            return;
        } else {
            // user has modified existing external intermediaries, should update them
            List<Long> persistedExternalIntermediaryIds = persistedExternalIntermediaries
                    .stream()
                    .map(ContractExternalIntermediary::getExternalIntermediaryId)
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

                    Optional<ContractExternalIntermediary> persistedExternalIntermediaryOptional = persistedExternalIntermediaries
                            .stream()
                            .filter(contractExternalIntermediary -> contractExternalIntermediary.getExternalIntermediaryId().equals(externalIntermediary))
                            .findFirst();
                    if (persistedExternalIntermediaryOptional.isEmpty()) {
                        log.error("additionalParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                        errorMessages.add("additionalParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                    } else {
                        ContractExternalIntermediary persistedExternalIntermediary = persistedExternalIntermediaryOptional.get();
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

                    createExternalIntermediary(productContractDetails, tempList, externalIntermediary);
                }
            }

            // if the external intermediary is not present in the request, we set its status to DELETED
            for (ContractExternalIntermediary externalIntermediary : persistedExternalIntermediaries) {
                if (!externalIntermediaries.contains(externalIntermediary.getExternalIntermediaryId())) {
                    externalIntermediary.setStatus(ContractSubObjectStatus.DELETED);
                    tempList.add(externalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            contractExternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Validates and creates assisting employees for the contract.
     *
     * @param assistingEmployees     list of assisting employee IDs
     * @param productContractDetails contract details object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void createAssistingEmployees(List<Long> assistingEmployees,
                                          ProductContractDetails productContractDetails,
                                          List<String> errorMessages) {
        if (CollectionUtils.isEmpty(assistingEmployees)) {
            // assisting employees are not mandatory
            return;
        }

        String duplicationValidation = EPBListUtils.validateDuplicateValuesByIndexes(assistingEmployees, "additionalParameters.assistingEmployees");
        if (StringUtils.isNotEmpty(duplicationValidation)) {
            errorMessages.add(duplicationValidation);
            return;
        }

        List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), assistingEmployees);

        List<ContractAssistingEmployee> tempList = new ArrayList<>();
        for (int i = 0; i < assistingEmployees.size(); i++) {
            Long assistingEmployee = assistingEmployees.get(i);
            if (!systemUsers.contains(assistingEmployee)) {
                log.error("additionalParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                errorMessages.add("additionalParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                continue;
            }

            createAssistingEmployee(assistingEmployee, productContractDetails, tempList);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            contractAssistingEmployeeRepository.saveAll(tempList);
        }
    }


    /**
     * Creates assisting employee for the contract.
     *
     * @param assistingEmployee      assisting employee ID
     * @param productContractDetails contract details object to be updated
     * @param tempList               list of assisting employees to be populated
     */
    private static void createAssistingEmployee(Long assistingEmployee, ProductContractDetails productContractDetails, List<ContractAssistingEmployee> tempList) {
        ContractAssistingEmployee assistant = new ContractAssistingEmployee();
        assistant.setStatus(ContractSubObjectStatus.ACTIVE);
        assistant.setAccountManagerId(assistingEmployee);
        assistant.setContractDetailId(productContractDetails.getId());
        tempList.add(assistant);
    }


    /**
     * Validates and updates assisting employees for the contract.
     *
     * @param assistingEmployees     list of assisting employee IDs
     * @param productContractDetails contract details object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void updateAssistingEmployees(List<Long> assistingEmployees,
                                          ProductContractDetails productContractDetails,
                                          List<String> errorMessages) {
        List<ContractAssistingEmployee> persistedAssistingEmployees = contractAssistingEmployeeRepository
                .findByContractDetailIdAndStatusIn(productContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));

        if (CollectionUtils.isEmpty(assistingEmployees)) {
            if (CollectionUtils.isNotEmpty(persistedAssistingEmployees)) {
                // user has removed all assisting employees, should set deleted status to them
                persistedAssistingEmployees.forEach(assistingEmployee -> assistingEmployee.setStatus(ContractSubObjectStatus.DELETED));
                contractAssistingEmployeeRepository.saveAll(persistedAssistingEmployees);
            }
            return;
        }

        List<ContractAssistingEmployee> tempList = new ArrayList<>();

        // at this moment we already know that assisting employees list is not empty
        if (CollectionUtils.isEmpty(persistedAssistingEmployees)) {
            // user has added new assisting employees, should create them
            createAssistingEmployees(assistingEmployees, productContractDetails, errorMessages);
            return;
        } else {
            // user has modified (added/edited) assisting employees, should update them
            List<Long> persistedAssistingEmployeeIds = persistedAssistingEmployees
                    .stream()
                    .map(ContractAssistingEmployee::getAccountManagerId)
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
                    createAssistingEmployee(assistingEmployee, productContractDetails, tempList);
                } else {
                    Optional<ContractAssistingEmployee> persistedAssistingEmployeeOptional = persistedAssistingEmployees
                            .stream()
                            .filter(assistingEmployee1 -> assistingEmployee1.getAccountManagerId().equals(assistingEmployee))
                            .findFirst();

                    if (persistedAssistingEmployeeOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                    } else {
                        ContractAssistingEmployee persistedAssistingEmployee = persistedAssistingEmployeeOptional.get();
                        persistedAssistingEmployee.setAccountManagerId(assistingEmployee);
                        tempList.add(persistedAssistingEmployee);
                    }
                }
            }

            // user has removed some assisting employees, should set deleted status to them
            for (ContractAssistingEmployee assistingEmployee : persistedAssistingEmployees) {
                if (!assistingEmployees.contains(assistingEmployee.getAccountManagerId())) {
                    assistingEmployee.setStatus(ContractSubObjectStatus.DELETED);
                    tempList.add(assistingEmployee);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            contractAssistingEmployeeRepository.saveAll(tempList);
        }
    }


    /**
     * Returns additional parameters information with the sub objects for the contract.
     *
     * @param productContractDetails contract details object to be updated
     * @return additional parameters information with the sub objects for the contract
     */
    public AdditionalParametersResponse getAdditionalParametersResponse(ProductContractDetails productContractDetails) {
        log.debug("Getting additional parameters for contract with ID %s".formatted(productContractDetails.getId()));

        AdditionalParametersResponse response = productContractDetailsRepository
                .getAdditionalParametersByProductContractDetailId(productContractDetails.getId());

        // NOTE: id field in the following objects represents the account manager id (in internal intermediaries and assisting employees)
        // and external intermediary id (in external intermediaries), and not a db record id.

        response.setInternalIntermediaries(
                contractInternalIntermediaryRepository
                        .getShortResponseByContractDetailIdAndStatusIn(
                                productContractDetails.getId(),
                                List.of(ContractSubObjectStatus.ACTIVE)
                        )
        );

        response.setExternalIntermediaries(
                contractExternalIntermediaryRepository
                        .getShortResponseByContractDetailIdAndStatusIn(
                                productContractDetails.getId(),
                                List.of(ContractSubObjectStatus.ACTIVE)
                        )
        );

        response.setAssistingEmployees(
                contractAssistingEmployeeRepository
                        .getShortResponseByContractDetailIdAndStatusIn(
                                productContractDetails.getId(),
                                List.of(ContractSubObjectStatus.ACTIVE)
                        )
        );

        Long contractId = productContractDetails.getContractId();

        response.setActivities(productContractActivityService.getActivitiesByConnectedObjectId(contractId));
        response.setTasks(taskService.getTasksByProductContractId(contractId));

        return response;
    }

}
