package bg.energo.phoenix.service.contract.expressContract;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractContractVersionTypes;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.Manager;
import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.entity.pod.pod.PodContractResponse;
import bg.energo.phoenix.model.entity.product.product.ProductContractProductListingResponse;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductSegments;
import bg.energo.phoenix.model.entity.product.service.ServiceContractServiceListingResponse;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.express.ExpressContractType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractParameters;
import bg.energo.phoenix.model.request.contract.express.ExpressContractRequest;
import bg.energo.phoenix.model.request.contract.express.ExpressContractServiceParametersRequest;
import bg.energo.phoenix.model.request.pod.pod.PodContractRequest;
import bg.energo.phoenix.model.request.product.product.ProductContractProductListingRequest;
import bg.energo.phoenix.model.request.product.service.ServiceContractProductListingRequest;
import bg.energo.phoenix.model.response.contract.express.CustomerExpressContractDto;
import bg.energo.phoenix.model.response.contract.express.ExpressContractCustomerShortResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPurposeDetailedResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractContractVersionTypesRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.product.ProductContractPodService;
import bg.energo.phoenix.service.contract.product.ProductContractProductParametersService;
import bg.energo.phoenix.service.contract.proxy.ProxyService;
import bg.energo.phoenix.service.contract.service.ServiceContractBasicParametersService;
import bg.energo.phoenix.service.contract.service.ServiceContractService;
import bg.energo.phoenix.service.contract.service.ServiceContractServiceParametersService;
import bg.energo.phoenix.service.customer.CustomerSegmentService;
import bg.energo.phoenix.service.pod.pod.PointOfDeliveryService;
import bg.energo.phoenix.service.product.product.ProductRelatedEntitiesService;
import bg.energo.phoenix.service.product.service.ServiceRelatedEntitiesService;
import bg.energo.phoenix.service.riskList.RiskListService;
import bg.energo.phoenix.service.riskList.model.RiskListBasicInfoResponse;
import bg.energo.phoenix.service.riskList.model.RiskListDecision;
import bg.energo.phoenix.service.riskList.model.RiskListRequest;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.contract.ContractUtils;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpressContractService {
    private final ContractUtils contractUtils;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final InterestRateRepository interestRateRepository;
    private final ServiceContractContractVersionTypesRepository serviceContractContractVersionTypesRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceContractsRepository serviceContractsRepository;

    private final ProductContractProductParametersService productParametersService;
    private final ServiceContractServiceParametersService serviceParametersService;
    private final ProductRepository productRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ExpressContractParametersService contractParametersService;
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ExpressContractCustomerService expressContractCustomerService;
    private final PointOfDeliveryService pointOfDeliveryService;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CustomerCommContactPurposesRepository customerCommContactPurposesRepository;
    private final CommunicationContactPurposeProperties productContractProperties;
    private final ProductContractPodService productContractPodService;
    private final PointOfDeliveryDetailsRepository podDetailRepository;
    private final RiskListService riskListService;
    private final CustomerRepository customerRepository;
    private final ProxyService proxyService;
    private final ProductRelatedEntitiesService productRelatedEntitiesService;
    private final ServiceRelatedEntitiesService serviceRelatedEntitiesService;
    private final PermissionService permissionService;
    private final AccountManagerRepository accountManagerRepository;
    private final ServiceContractService serviceContractService;
    private final ContractVersionTypesRepository contractVersionTypesRepository;
    private final ServiceContractBasicParametersService serviceContractBasicParametersService;
    private final ManagerRepository managerRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerSegmentService customerSegmentService;

    @Transactional
    public Long create(ExpressContractRequest request) {
        List<String> messages = new ArrayList<>();
        CustomerExpressContractDto customerDetailId = expressContractCustomerService.create(request.getCustomer());
        if (request.getExpressContractType().equals(ExpressContractType.PRODUCT)) {
            return createExpressContractForProduct(request, customerDetailId, messages);
        } else if (request.getExpressContractType().equals(ExpressContractType.SERVICE)) {
            return createExpressContractForService(request, customerDetailId, messages);
        }
        return -1L;
    }


    // TODO: 10/9/23 implement later
    private Long createExpressContractForService(ExpressContractRequest request, CustomerExpressContractDto customerDto, List<String> errorMessages) {
        ExpressContractParameters expressContractParameters = request.getExpressContractParameters();
        ServiceContracts serviceContracts = createServiceContract(expressContractParameters);
        ServiceContractDetails serviceContractDetails = contractParametersService.createForService(expressContractParameters, serviceContracts, customerDto, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        serviceParametersService.createForExpress(request, serviceContractDetails, serviceContracts, errorMessages);
        processEmployeeForServiceExpressContract(serviceContractDetails, errorMessages);
        List<Long> list = customerCommunicationsRepository.findByCustomerDetailIdAndStatuses(customerDto.getCustomerDetailId(), List.of(Status.ACTIVE)).stream().map(x -> x.getId()).toList();
        List<ContactPurposeDetailedResponse> contactDetailedResponses = customerCommContactPurposesRepository.findByCustomerCommIds(list, List.of(Status.ACTIVE));
        for (ContactPurposeDetailedResponse contactDetails : contactDetailedResponses) {
            if (contactDetails.getId().equals(productContractProperties.getContractCommunicationId())) {
                serviceContractDetails.setCustomerCommunicationIdForContract(contactDetails.getCustomerCommunicationsId());
            } else if (contactDetails.getId().equals(productContractProperties.getBillingCommunicationId())) {
                serviceContractDetails.setCustomerCommunicationIdForBilling(contactDetails.getCustomerCommunicationsId());
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        serviceContractDetails.setType(ServiceContractContractType.CONTRACT);
        serviceContractDetails.setContractVersionStatus(ContractVersionStatus.READY);//TODO ASK task says: Automatically set Contract
        serviceContractDetails.setContractVersionStatus(ContractVersionStatus.READY);//TODO ASK task says: Automatically set Contract
        serviceContractDetails.setApplicableInterestRate(createInterestRates(errorMessages));
        serviceContractDetails.setGuaranteeContract(false);
        serviceContractDetails.setGuaranteeContractInfo(null);
        serviceContractDetails.setCampaignId(getCampaignId(request.getExpressContractParameters().getCampaignId()));
        ServiceContractDetails save = serviceContractDetailsRepository.save(serviceContractDetails);
        createContractVersionTypes(save, errorMessages);
        serviceContractBasicParametersService.createServiceContractProxy(getCustomer(customerDto), customerDto.getVersion(), request.getProxyRequest(), serviceContractDetails, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        serviceParametersService.fillSubActivityDetails(request.getServiceParameters(), serviceContractDetails, errorMessages);
        checkSubObjectsAccordingToExecutionLevel(serviceContracts, request.getServiceParameters(), serviceContractDetails, errorMessages);
        serviceParametersService.createSubObjects(serviceContracts, request.getServiceParameters(), serviceContractDetails, errorMessages);

        // TODO: 10/9/23 uncomment after implementation (should be last statements - before creation of contract)
        if (!serviceRelatedEntitiesService.canCreateServiceContractWithServiceAndCustomer(
                request.getExpressContractParameters().getProductId(),
                request.getExpressContractParameters().getProductVersionId(),
                getCustomerId(request.getCustomer().getIdentifier(), errorMessages),
                errorMessages
        )) {
            log.error("You are not allowed to create a contract because the service has related dependencies.");
            errorMessages.add("You are not allowed to create a contract because the service has related dependencies.");
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return save.getContractId();
    }

    private Long getCampaignId(Long campaignId) {
        Optional<Campaign> campaignOptional = campaignRepository.findByIdAndStatusIn(campaignId, List.of(NomenclatureItemStatus.ACTIVE));
        return campaignOptional.map(Campaign::getId).orElse(null);
    }

    private void checkSubObjectsAccordingToExecutionLevel(ServiceContracts serviceContracts, ExpressContractServiceParametersRequest serviceParameters, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        Optional<ServiceDetails> serviceDetailsOptional = serviceDetailsRepository.findByIdAndStatus(serviceContractDetails.getServiceDetailId(), ServiceDetailStatus.ACTIVE);
        if (serviceDetailsOptional.isPresent()) {
            ServiceDetails serviceDetails = serviceDetailsOptional.get();
            if (serviceDetails.getExecutionLevel().equals(ServiceExecutionLevel.CONTRACT)) {
                if (CollectionUtils.isEmpty(serviceParameters.getContractNumbers())) {
                    errorMessages.add("serviceParameters.contractNumbers- [contractNumbers] can't be empty when Service execution level is CONTRACT;");
                }
            } else if (serviceDetails.getExecutionLevel().equals(ServiceExecutionLevel.POINT_OF_DELIVERY)) {
                if (CollectionUtils.isEmpty(serviceParameters.getPodIds()) && CollectionUtils.isEmpty(serviceParameters.getUnrecognizedPods())) {
                    errorMessages.add("serviceParameters.podIds- [podIds] or unrecognized pods should be filled in when service execution level is POD;" +
                            "serviceParameters.unrecognizedPods- [unrecognizedPods] or podIds should be filled in when service execution level is POD;");
                }
            }
        }
    }


    private Customer getCustomer(CustomerExpressContractDto request) {
        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(request.getCustomerId(), List.of(CustomerStatus.ACTIVE));
        return customerOptional.orElse(null);
    }

    private Long createInterestRates(List<String> errorMessages) {
        Optional<InterestRate> interestRateOptional = interestRateRepository.findByIsDefaultAndStatus(true, InterestRateStatus.ACTIVE);
        if (interestRateOptional.isPresent()) {
            return interestRateOptional.get().getId();
        } else {
            errorMessages.add("Contract can't be created because 'applicable interest rate' doesn't have default value");
            return null;
        }
    }

    private void createContractVersionTypes(ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        Optional<ContractVersionType> versionTypeOptional = contractVersionTypesRepository.findByIsDefaultTrue();
        if (versionTypeOptional.isPresent()) {
            ServiceContractContractVersionTypes serviceContractContractVersionTypes = new ServiceContractContractVersionTypes();
            serviceContractContractVersionTypes.setStatus(ContractSubObjectStatus.ACTIVE);
            serviceContractContractVersionTypes.setContractVersionTypeId(versionTypeOptional.get().getId());
            serviceContractContractVersionTypes.setContractDetailId(serviceContractDetails.getId());
            serviceContractContractVersionTypesRepository.save(serviceContractContractVersionTypes);
        } else {
            errorMessages.add("Contract can't be created because 'Contract version type' doesn't have default value;");
        }
    }


    public ExpressContractCustomerShortResponse findCustomerForContract(String identifier) {
        return expressContractCustomerService.getCustomerShortResponse(identifier);
    }

    private Long createExpressContractForProduct(ExpressContractRequest request, CustomerExpressContractDto customerDto, List<String> errorMessages) {
        //Write product contract create logic here!;
        validateSegment(request, errorMessages);
        ExpressContractParameters expressContractParameters = request.getExpressContractParameters();
        ProductContract productContract = createProductContract(expressContractParameters);
        ProductContractDetails productContractDetails = contractParametersService.createProductContractDetail(
                expressContractParameters,
                productContract,
                customerDto.getCustomerDetailId(),
                errorMessages
        );
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        productParametersService.createForExpress(request, productContractDetails, productContract, errorMessages);
        validateCustomerInRiskListAPI(customerDto.getIdentifier(), customerDto.getVersion(), expressContractParameters.getEstimatedTotalConsumption(), productContractDetails, errorMessages);
        processEmployeeOnCreate(productContractDetails, errorMessages);
        List<Long> list = customerCommunicationsRepository.findByCustomerDetailIdAndStatuses(customerDto.getCustomerDetailId(), List.of(Status.ACTIVE)).stream().map(x -> x.getId()).toList();
        List<ContactPurposeDetailedResponse> contactDetailedResponses = customerCommContactPurposesRepository.findByCustomerCommIds(list, List.of(Status.ACTIVE));
        for (ContactPurposeDetailedResponse contactDetails : contactDetailedResponses) {
            if (contactDetails.getId().equals(productContractProperties.getContractCommunicationId())) {
                productContractDetails.setCustomerCommunicationIdForContract(contactDetails.getCustomerCommunicationsId());
            } else if (contactDetails.getId().equals(productContractProperties.getBillingCommunicationId())) {
                productContractDetails.setCustomerCommunicationIdForBilling(contactDetails.getCustomerCommunicationsId());
            }
        }

        List<Long> podDetailIds = request.getPodDetailIds();
        validateConsumption(podDetailIds, expressContractParameters.getEstimatedTotalConsumption(), errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        ProductContractDetails save = productContractDetailsRepository.save(productContractDetails);
        List<Manager> managers = managerRepository.findManagersByCustomerDetailId(customerDto.getCustomerDetailId(), List.of(Status.ACTIVE));
        List<ProxyEditRequest> proxyRequest = request.getProxyRequest();
        if (!proxyRequest.isEmpty()) {
            for (ProxyEditRequest item : proxyRequest) {
                Set<Long> managerIds = new HashSet<>();
                for (Manager itemManager : managers) {
                    managerIds.add(itemManager.getId());
                }
                item.setManagerIds(managerIds);
            }
        }
        proxyService.createProxies(/*getCustomerId(request.getCustomer().getIdentifier(),errorMessages)*/customerDto.getCustomerId(), customerDto.getVersion(), request.getProxyRequest(), save.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        productParametersService.fillSubActivityDetails(request.getProductParameters(), save, errorMessages);
        contractParametersService.setVersionType(save);
        productContractPodService.addPodsToContract(podDetailIds,
                expressContractParameters.getProductId(),
                expressContractParameters.getProductVersionId(),
                save,
                errorMessages
        );

        if (!productRelatedEntitiesService.canCreateProductContractWithProductVersionAndCustomer(
                request.getExpressContractParameters().getProductId(),
                request.getExpressContractParameters().getProductVersionId(),
                getCustomerId(request.getCustomer().getIdentifier(), errorMessages),
                errorMessages)
        ) {
            log.error("You are not allowed to create a contract because the product has related dependencies.");
            errorMessages.add("You are not allowed to create a contract because the product has related dependencies.");
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return save.getContractId();
    }

    private void validateSegment(ExpressContractRequest request, List<String> errorMessages) {
        if (!customerSegmentService.hasPermission(PermissionEnum.CUSTOMER_EDIT_SEGMENT)) {
            return;
        }

        boolean isSegmentValid = false;
        Set<Long> customerSegments = request.getCustomer().getCustomerSegments();
        Optional<ProductDetails> productDetailsOptional = productDetailsRepository.findByProductIdAndVersionAndStatus(request.getExpressContractParameters().getProductId(),
                request.getExpressContractParameters().getProductVersionId(),
                List.of(ProductDetailStatus.ACTIVE));
        if (productDetailsOptional.isPresent()) {
            ProductDetails productDetails = productDetailsOptional.get();
            if (productDetails.getGlobalSegment() && customerSegments != null && !customerSegments.isEmpty()) {
                isSegmentValid = true;
            }
            List<ProductSegments> productSegments = productDetails.getSegments();
            for (ProductSegments productSegment : productSegments) {
                if (customerSegments.contains(productSegment.getSegment().getId())) {
                    isSegmentValid = true;
                    break;
                }
            }
        }
        if (!isSegmentValid) {
            errorMessages.add("customer and product does not have common segments;");
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        }
    }

    private void processEmployeeOnCreate(ProductContractDetails productContractDetails, List<String> errorMessages) {
        AccountManager accountManager = getAccountManager();
        productContractDetails.setEmployeeId(accountManager.getId());
    }

    private void processEmployeeForServiceExpressContract(ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        AccountManager accountManager = getAccountManager();
        serviceContractDetails.setEmployeeId(accountManager.getId());
    }

    private AccountManager getAccountManager() {
        String loggedInUserName = permissionService.getLoggedInUserId();
        return accountManagerRepository.findByUserNameAndStatusIn(loggedInUserName, List.of(Status.ACTIVE))
                .orElseThrow(() -> new ClientException("Logged user do not exist as account manager!;", ErrorCode.APPLICATION_ERROR));
    }

    private Long getCustomerId(String identifier, List<String> messages) {
        Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(identifier, CustomerStatus.ACTIVE);
        if (customer.isPresent()) {
            return customer.get().getId();
        } else {
            messages.add("Can't find Active Customer with identifier:%s;".formatted(identifier));
            return null;
        }
    }

    private void validateCustomerInRiskListAPI(String customerIdentifier,
                                               Long customerVersion,
                                               BigDecimal estimatedContractKwh,
                                               ProductContractDetails productContractDetails,
                                               List<String> errorMessages) {


        RiskListRequest riskListRequest = new RiskListRequest(
                customerIdentifier,
                customerVersion,
                estimatedContractKwh
        );

        RiskListBasicInfoResponse riskListBasicInfoResponse = riskListService.evaluateBasicCustomerRisk(riskListRequest);

        // value returned from the API on "save" button is having priority over the one [if] provided in the request
        RiskListDecision decision = riskListBasicInfoResponse.getDecision();
        if (decision != null) {
            if (decision.equals(RiskListDecision.PERMIT)) {
                // only "PERMITTED" risk list decisions are allowed to proceed
                productContractDetails.setRiskAssessment(riskListBasicInfoResponse.getDecision().getValue());
                productContractDetails.setRiskAssessmentAdditionalCondition(
                        riskListBasicInfoResponse.getRecommendations().isEmpty() ? null : String.join(";", riskListBasicInfoResponse.getRecommendations())
                );
            } else {
                log.error("additionalParameters.riskAssessment-Not allowed to conclude a contract with the customer with UIC/PN %s;"
                        .formatted(customerIdentifier));
                // in case of a deny, frontend needs the error message to be populated by the decision and recommendations
                // (hence, the custom prefix indicators are needed)
                errorMessages.add("%s-Not allowed to conclude a contract with the customer with UIC/PN %s;"
                        .formatted(EPBFinalFields.RISK_LIST_DECISION_INDICATOR, customerIdentifier));
                if (CollectionUtils.isNotEmpty(riskListBasicInfoResponse.getRecommendations())) {
                    errorMessages.add("%s-%s".formatted(
                            EPBFinalFields.RISK_LIST_RECOMMENDATIONS_INDICATOR,
                            String.join(";", riskListBasicInfoResponse.getRecommendations()))
                    );
                }
            }
        }
    }

    private void validateConsumption(List<Long> podDetailIds, BigDecimal consumption, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(podDetailIds)) {
            List<Integer> podConsumptions = podDetailRepository.findEstimatedMonthlyAvgConsumptionByIdIn(podDetailIds);
            int totalConsumption = podConsumptions.stream().mapToInt(Integer::intValue).sum();
            BigDecimal summedConsumption = BigDecimal.valueOf(totalConsumption * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED);
            if (!consumption.equals(summedConsumption)) {
                log.error("additionalParameters.estimatedTotalConsumptionUnderContractKwh-Value should match summed up value of estimated monthly average consumption of the selected PODs;");
                errorMessages.add("additionalParameters.estimatedTotalConsumptionUnderContractKwh-Value should match summed up value of estimated monthly average consumption of the selected PODs;");
            }
        }
    }

    private ProductContract createProductContract(ExpressContractParameters request) {
        ProductContract productContract = new ProductContract(ProductContractStatus.ACTIVE);

        productContract.setContractNumber(contractUtils.getNextContractNumber());
        productContract.setStatus(ProductContractStatus.ACTIVE);
        if (request.getSigningDate() == null) {
            productContract.setContractStatus(ContractDetailsStatus.READY);
            productContract.setSubStatus(ContractDetailsSubStatus.READY);
        } else {
            productContract.setContractStatus(ContractDetailsStatus.SIGNED);
            productContract.setSubStatus(ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES);
        }

        productContract.setStatusModifyDate(LocalDate.now());
        productContract.setSigningDate(request.getSigningDate());

        return productContractRepository.saveAndFlush(productContract);
    }

    private ServiceContracts createServiceContract(ExpressContractParameters expressContractParameters) {
        ServiceContracts serviceContracts = new ServiceContracts();

        if (expressContractParameters.getSigningDate() == null) {
            serviceContracts.setContractStatus(ServiceContractDetailStatus.READY);
            serviceContracts.setSubStatus(ServiceContractDetailsSubStatus.READY);
        } else {
            serviceContracts.setContractStatus(ServiceContractDetailStatus.SIGNED);
            serviceContracts.setSubStatus(ServiceContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES);
        }

        serviceContracts.setContractNumber(contractUtils.getNextContractNumber());

        serviceContracts.setStatus(EntityStatus.ACTIVE);
        serviceContracts.setStatusModifyDate(LocalDate.now());
        serviceContracts.setSigningDate(expressContractParameters.getSigningDate());
        return serviceContractsRepository.saveAndFlush(serviceContracts);
    }

    public ContractPodsResponse createForContract(PodContractRequest request) {
        return pointOfDeliveryService.createForContract(request);
    }

    public PodContractResponse getContractResponse(String identifier) {
        return pointOfDeliveryService.getContractResponse(identifier);
    }

    public Page<ProductContractProductListingResponse> getProductsForExpress(ProductContractProductListingRequest request) {

        return productRepository.searchForExpressContract(
                        request.getCustomerDetailId(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt() == null ? "" : request.getPrompt()),
                        permissionService.getLoggedInUserId(),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "customer_identifier", "pd.name")
                        ))
                .map(ProductContractProductListingResponse::new);
    }

    public Page<ServiceContractServiceListingResponse> serviceListingForContract(ServiceContractProductListingRequest request) {
        return serviceContractService.serviceListingForContractForExpressContract(request);
    }
}
