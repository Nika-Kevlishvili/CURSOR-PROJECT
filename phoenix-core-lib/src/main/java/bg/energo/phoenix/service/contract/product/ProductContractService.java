package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.product.ProductContractSignableDocuments;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.product.product.ProductContractProductListingResponse;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.products.*;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.product.ExcludeVersions;
import bg.energo.phoenix.model.request.contract.product.*;
import bg.energo.phoenix.model.request.product.product.ProductContractProductListingRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponse;
import bg.energo.phoenix.model.response.contract.productContract.*;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractSignableDocumentRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.template.ProductTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.activity.ProductContractActivityService;
import bg.energo.phoenix.service.contract.billing.BillingGroupService;
import bg.energo.phoenix.service.contract.product.dealCreationEvent.ProductContractDealCreationEvent;
import bg.energo.phoenix.service.contract.product.dealCreationEvent.ProductContractDealCreationEventPublisher;
import bg.energo.phoenix.service.contract.product.models.ProductContractCreationPayload;
import bg.energo.phoenix.service.contract.product.resign.ProductContractResignService;
import bg.energo.phoenix.service.contract.product.startDateUpdate.ProductContractStartDateUpdateRoute;
import bg.energo.phoenix.service.contract.proxy.ProxyService;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.product.ProductRelatedEntitiesService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import bg.energo.phoenix.util.contract.ContractUtils;
import bg.energo.phoenix.util.contract.product.ProductContractStatusChainUtil;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus.SIGNED;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PRODUCT_CONTRACTS;
import static bg.energo.phoenix.permissions.PermissionEnum.PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS;
import static bg.energo.phoenix.permissions.PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductContractService {
    private final ProductTemplateRepository productTemplateRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final CustomerRepository customerRepository;

    private final ProductContractBasicParametersService basicParametersService;
    private final ProductContractAdditionalParametersService additionalParametersService;
    private final ProductContractPodService productContractPodService;

    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductContractProductParametersService productParametersService;
    private final ProductContractActivityService productContractActivityService;
    private final ProxyService proxyService;
    private final BillingGroupService billingGroupService;
    private final PermissionService permissionService;
    private final TaskService taskService;
    private final ProductRelatedEntitiesService productRelatedEntitiesService;
    private final List<ProductContractStartDateUpdateRoute> startDateUpdateRouters;
    private final ContractUtils contractUtils;
    private final ProductContractResignService productContractResignService;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final ProductContractDealCreationEventPublisher productContractDealCreationEventPublisher;
    private final ContractPodRepository contractPodRepository;
    private final XEnergieRepository xEnergieRepository;
    private final TransactionTemplate transactionTemplate;
    private final ProductContractPodContinuityValidationService productContractPodContinuityValidationService;
    private final ProductContractBillingLockValidationService productContractBillingLockValidationService;
    private final SignerChainManager signerChainManager;
    private final ProductContractSignableDocumentRepository signableDocumentRepository;


    private static void fillContractStatus(ProductContractCreateRequest request, ProductContract productContract) {
        ProductContractBasicParametersCreateRequest basicParameters = request.getBasicParameters();
        productContract.setContractStatus(basicParameters.getStatus());
        productContract.setSubStatus(basicParameters.getSubStatus());
        if (basicParameters.getStatusModifyDate() == null) {
            productContract.setStatusModifyDate(LocalDate.now());
        } else {
            productContract.setStatusModifyDate(basicParameters.getStatusModifyDate());
        }
        productContract.setSigningDate(basicParameters.getSigningDate());
        productContract.setInitialTermDate(basicParameters.getStartOfInitialTerm());
    }

    /**
     * Creates a new product contract with all its sub-objects.
     *
     * @param request The request containing the data for the product contract.
     * @return The ID of the created product contract.
     */
    public ProductContractDataModificationResponse create(ProductContractCreateRequest request) {
        log.debug("Creating product contract with request: {}", request);
        List<String> errorMessages = new ArrayList<>();

        ProductContractCreationPayload payload = transactionTemplate.execute(res -> createProductContract(request, errorMessages));

        if (Objects.nonNull(payload)) {
            List<String> resigningMessages = new ArrayList<>(startResigningProcess(request, errorMessages, payload.productContract(), payload.productContractDetails()));

            if (payload.productContractDetails().getCustomerDetailId() != null) {
                try {
                    customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(List.of(payload.productContractDetails().getCustomerDetailId()));
                } catch (Exception e) {
                    log.debug("Exception handled while trying to publish customer status change event", e);
                }
            }

            if (request.getBasicParameters().getVersionStatus() == SIGNED) {
                try {
                    productContractDealCreationEventPublisher.publishProductContractDealCreationEvent(
                            new ProductContractDealCreationEvent(payload.productContractDetails()));
                } catch (Exception e) {
                    log.debug("Exception handled while trying to publish product contract deal creation event", e);
                }
            }

            return new ProductContractDataModificationResponse(payload.productContract().getId(), resigningMessages);
        }

        return null;
    }

    public ProductContractCreationPayload createProductContract(ProductContractCreateRequest request, List<String> errorMessages) {
        ProductContract productContract = new ProductContract(ProductContractStatus.ACTIVE);
        fillContractStatus(request, productContract);
        productContract.setContractNumber(contractUtils.getNextContractNumber());
        productContractRepository.saveAndFlush(productContract);

        ProductContractDetails productContractDetails = new ProductContractDetails();
        basicParametersService.create(request, productContract, errorMessages, productContractDetails);
        additionalParametersService.create(request, productContractDetails, errorMessages);
        productParametersService.create(request, productContractDetails, productContract, errorMessages);
        calculateAdditionalAgreementSuffix(productContractDetails, productContract);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        productContractDetails = productContractDetailsRepository.saveAndFlush(productContractDetails);

        basicParametersService.createBasicParameterSubObjects(request.getBasicParameters(), productContractDetails, errorMessages);
        additionalParametersService.createAdditionalParametersSubObjects(request, productContractDetails, errorMessages);
        productParametersService.fillSubActivityDetails(request.getProductParameters(), productContractDetails, errorMessages);
        productParametersService.createProductAdditionalParams(request.getProductParameters(), productContractDetails, errorMessages);

        productContractPodService.addPodsToContract(
                request.getProductContractPointOfDeliveries().stream().map(ProductContractPointOfDeliveryRequest::pointOfDeliveryDetailId).toList(),
                request.getBasicParameters().getProductId(),
                request.getBasicParameters().getProductVersionId(),
                productContractDetails, errorMessages
        );

        if (!productRelatedEntitiesService.canCreateProductContractWithProductVersionAndCustomer(
                request.getBasicParameters().getProductId(),
                request.getBasicParameters().getProductVersionId(),
                request.getBasicParameters().getCustomerId(),
                errorMessages)
        ) {
            log.error("You are not allowed to create a contract because the product has related dependencies.");
            errorMessages.add("You are not allowed to create a contract because the product has related dependencies.");
        }
        proxyService.createProxies(request.getBasicParameters().getCustomerId(), request.getBasicParameters().getCustomerVersionId(), request.getBasicParameters().getProxy(), productContractDetails.getId(), errorMessages);
        setProductContractPointOfDeliveryDealManuallyOnCreate(productContractDetails, request.getProductContractPointOfDeliveries(), errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return new ProductContractCreationPayload(productContract, productContractDetails);
    }

    @Transactional
    public ProductContractDataModificationResponse edit(Long contractId,
                                                        Integer versionId,
                                                        ProductContractUpdateRequest updateRequest,
                                                        Boolean isRequestedChangeFutureVersionsPods) {
        log.debug("Editing product contract with id: {}, version id: {}", contractId, versionId);

        if (versionId != null && versionId == 1
                && !updateRequest.isSavingAsNewVersion()
                && updateRequest.getBasicParameters().getVersionStatus() != SIGNED
        ) {
            throw new OperationNotAllowedException("Version status of first version should be VALID/SIGNED!");
        }

        ProductContract productContract = productContractRepository
                .findByIdAndStatusIn(contractId, getCustomerStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product not found!;"));
        ContractDetailsSubStatus initialSubStatus = productContract.getSubStatus();

        if (productContract.getLocked() != null && productContract.getLocked()) {
            throw new ClientException("Contract is locked by automatic process in xEnergie!;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        boolean hasAdditionalAgreementPermission = permissionService.permissionContextContainsPermissions(
                PRODUCT_CONTRACTS,
                List.of(PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS)
        );

        if (isLockedByInvoice(contractId)) {
            if (
                    productContractBillingLockValidationService.isRestrictedForAdditionalAgreementPermission(
                            contractId,
                            updateRequest.getStartDate(),
                            hasAdditionalAgreementPermission
                    )
            ) {
                if (!permissionService.permissionContextContainsPermissions(
                        PermissionContextEnum.PRODUCT_CONTRACTS,
                        List.of(
                                PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED
                        )
                )
                ) {
                    throw new ClientException("Contract is locked by invoice!;", ErrorCode.OPERATION_NOT_ALLOWED);
                }
            }
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            if (!List.of(ContractDetailsStatus.DRAFT, ContractDetailsStatus.READY).contains(productContract.getContractStatus()) &&
                    !permissionService.permissionContextContainsPermissions(
                            PermissionContextEnum.PRODUCT_CONTRACTS,
                            List.of(
                                    PermissionEnum.PRODUCT_CONTRACT_EDIT,
                                    PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED,
                                    PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS
                            )
                    )
            ) {
                throw new ClientException("You can not edit this contract!;", ErrorCode.ACCESS_DENIED);
            }
            if (productContract.getContractStatus().equals(ContractDetailsStatus.READY) &&
                    !permissionService.permissionContextContainsPermissions(
                            PermissionContextEnum.PRODUCT_CONTRACTS,
                            List.of(
                                    PermissionEnum.PRODUCT_CONTRACT_EDIT_READY,
                                    PermissionEnum.PRODUCT_CONTRACT_EDIT,
                                    PRODUCT_CONTRACT_EDIT_ADDITIONAL_AGREEMENTS
                            )
                    )
            ) {
                throw new ClientException("You can not edit this contract!;", ErrorCode.ACCESS_DENIED);
            }
        }

        ProductContractDetails sourceDetails;
        if (versionId == null) {
            sourceDetails = productContractDetailsRepository
                    .findLatestProductContractDetails(contractId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("versionId-Product version not found!;"));
        } else {
            sourceDetails = productContractDetailsRepository
                    .findByContractIdAndVersionId(contractId, versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("versionId-Product version not found!;"));
        }
        boolean versionStatusIsChangedFromValidToNotValid = false;

        if (sourceDetails.getVersionStatus() == SIGNED
                && updateRequest.getBasicParameters().getVersionStatus() != SIGNED) {
            versionStatusIsChangedFromValidToNotValid = true;
        }


        String oldDealNumber = sourceDetails.getDealNumber();

        List<String> errorMessages = new ArrayList<>();

        ProductContractDetails detailsUpdating = basicParametersService.edit(
                productContract,
                updateRequest,
                sourceDetails,
                errorMessages
        );

        if (versionStatusIsChangedFromValidToNotValid) {
            detailsUpdating.setEndDate(null);
        }

        additionalParametersService.update(updateRequest, sourceDetails, detailsUpdating, errorMessages);
        productParametersService.update(updateRequest, detailsUpdating, productContract, errorMessages);
        updateContractStatusAndSendFilesForSignIfApplicable(productContract, updateRequest, detailsUpdating.getVersionId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        detailsUpdating = productContractDetailsRepository.saveAndFlush(detailsUpdating);
        productParametersService.fillProductAdditionalParams(updateRequest.getProductParameters(), detailsUpdating, errorMessages);

        if (updateRequest.isSavingAsNewVersion()) {
            proxyService.createProxies(updateRequest.getBasicParameters().getCustomerId(), updateRequest.getBasicParameters().getCustomerVersionId(), updateRequest.getBasicParameters().getProxy(), detailsUpdating.getId(), errorMessages);
        } else {
            proxyService.updateProxies(updateRequest.getBasicParameters().getCustomerId(), updateRequest.getBasicParameters().getCustomerVersionId(), updateRequest.getBasicParameters().getProxy(), sourceDetails.getId(), errorMessages);
        }
        calculateAdditionalAgreementSuffix(detailsUpdating, productContract);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        productParametersService.updateSubObjects(updateRequest, detailsUpdating, errorMessages);
        additionalParametersService.updateAdditionalParametersSubObjects(updateRequest, sourceDetails, detailsUpdating, errorMessages);

        //proxyService.updateProxies(updateRequest.getBasicParameters().getCustomerId(), updateRequest.getBasicParameters().getProxy(), detailsUpdating.getId(), errorMessages);
        basicParametersService.updateVersionTypes(sourceDetails, detailsUpdating, updateRequest.getBasicParameters(), errorMessages);
        //adding pods to contract
        productContractPodService.editProductContractPodsNew(
                detailsUpdating, // new version
                sourceDetails, // old version if new version is requested
                updateRequest.getPodRequests(),
                updateRequest.getBasicParameters().getProductId(),
                updateRequest.getBasicParameters().getProductVersionId(),
                errorMessages);
        //findVersionBefore but signed
        Optional<ProductContractVersionShortDto> versionBefore = productContractDetailsRepository.findVersionBefore(
                        detailsUpdating.getContractId(),
                        detailsUpdating.getStartDate(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst();
        if (versionBefore.isPresent() && updateRequest.getBasicParameters().getVersionStatus() == SIGNED) {
            ProductContractVersionShortDto productContractVersionShortDto = versionBefore.get();
            productContractPodService.adjustPodActivation(detailsUpdating,
                    productContractVersionShortDto,
                    !detailsUpdating.getId().equals(sourceDetails.getId()),
                    errorMessages);

        }
        if (updateRequest.getBasicParameters().getVersionStatus() != SIGNED) {
            Optional<ProductContractDetails> lastSignedDetail = productContractDetailsRepository
                    .findLastSignedProductContractVersion(
                            sourceDetails.getContractId(),
                            PageRequest.of(0, 1)
                    );
            if (lastSignedDetail.isPresent()) {
                ProductContractDetails lastSigned = lastSignedDetail.get();
                if (lastSigned.getEndDate() != null) {
                    lastSigned.setEndDate(null);
                }
            }
        }
        if (!updateRequest.isSavingAsNewVersion() && sourceDetails.getVersionId() == 1) {
            if (productContract.getSigningDate() != null
                    && productContract.getSigningDate().isBefore(LocalDate.now())
                    && !updateRequest.getStartDate().equals(productContract.getSigningDate())
                    && !updateRequest.getStartDate().equals(sourceDetails.getStartDate())
            ) {
                errorMessages.add("startDate- Incorrect start date;");
            } else {
                sourceDetails.setStartDate(updateRequest.getStartDate());
            }
        } else {
            updateStartDate(contractId, updateRequest, sourceDetails, errorMessages);
        }

        if (updateRequest.isSavingAsNewVersion()) {
            setProductContractPointOfDeliveryDealManuallyOnUpdate(detailsUpdating, updateRequest.getPodRequests(), errorMessages);
        } else {
            setProductContractPointOfDeliveryDealManuallyOnUpdate(sourceDetails, updateRequest.getPodRequests(), errorMessages);
        }

        basicParametersService.updateProductContractFiles(updateRequest, sourceDetails, detailsUpdating, errorMessages);
        basicParametersService.archiveFiles(detailsUpdating);

        basicParametersService.updateProductContractDocuments(updateRequest, sourceDetails, detailsUpdating, errorMessages);
        basicParametersService.archiveDocuments(detailsUpdating);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        List<String> resigningMessages = startResigningProcess(updateRequest,
                errorMessages,
                initialSubStatus,
                productContract,
                detailsUpdating,
                sourceDetails);

        Set<Long> customerDetailIDsTOCheck = new HashSet<>();
        if (sourceDetails.getCustomerDetailId() != null)
            customerDetailIDsTOCheck.add(sourceDetails.getCustomerDetailId());
        if (detailsUpdating.getCustomerDetailId() != null)
            customerDetailIDsTOCheck.add(detailsUpdating.getCustomerDetailId());

        if (!customerDetailIDsTOCheck.isEmpty())
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerDetailIDsTOCheck.stream().toList());

        if (updateRequest.getBasicParameters().getVersionStatus() == SIGNED) {
            if (!updateRequest.isSavingAsNewVersion()) {
                if (detailsUpdating.getId().equals(sourceDetails.getId())
                        && ((detailsUpdating.getDealNumber() != null
                        && !detailsUpdating.getDealNumber().equals(oldDealNumber))
                        || (detailsUpdating.getDealNumber() == null && oldDealNumber != null))) {
                    List<ContractPods> contractPods = productContractPodService.getPodsThatHaveActivationDate(sourceDetails.getId(), EntityStatus.ACTIVE);
                    for (ContractPods cp : contractPods) {
                        cp.setCustomModifyDate(LocalDateTime.now());
                    }
                    productContractPodService.saveAll(contractPods);
                }
            } else {
                if (updateRequest.isUpdateDealNumber()) {
                    //TODO: needs to be checked if there are already created contract pods for new version
                    List<ContractPods> contractPods = productContractPodService.getPodsThatHaveActivationDate(detailsUpdating.getId(), EntityStatus.ACTIVE);
                    for (ContractPods cp : contractPods) {
                        cp.setCustomModifyDate(LocalDateTime.now());
                    }
                    productContractPodService.saveAll(contractPods);
                }
            }

            Boolean hasEditLockedPermission = permissionService.permissionContextContainsPermissions(
                    PRODUCT_CONTRACTS,
                    List.of(PRODUCT_CONTRACT_EDIT_LOCKED)
            );
            if (!productContractBillingLockValidationService.isRestricted(
                    detailsUpdating.getContractId(),
                    updateRequest.getStartDate(),
                    hasEditLockedPermission)) {

                if (isRequestedChangeFutureVersionsPods) {
                    ProductContractDetails previousVersion = productContractPodContinuityValidationService
                            .findPreviousVersion(
                                    detailsUpdating.getContractId(),
                                    detailsUpdating.getStartDate()
                            );
                    ProductContractDetails nextVersion = productContractPodContinuityValidationService
                            .findNextVersion(
                                    detailsUpdating.getContractId(),
                                    detailsUpdating.getStartDate()
                            );
                    if (previousVersion != null && nextVersion != null) {
                        productContractPodContinuityValidationService.validatePodContinuity(
                                previousVersion.getContractId(),
                                previousVersion.getId(),
                                detailsUpdating.getId(),
                                nextVersion.getId(),
                                detailsUpdating.getStartDate()
                        );
                    }
                }
            }
        }
        if (updateRequest.getBasicParameters().getVersionStatus() != SIGNED) {
            //if version is draft should not be pod activation deactivation dates
            removeStartAndEndDatesFromPodsByContractDetail(detailsUpdating.getId());
        }

        if (updateRequest.getBasicParameters().getVersionStatus() == SIGNED) {
            productContractDealCreationEventPublisher
                    .publishProductContractDealCreationEvent(new ProductContractDealCreationEvent(detailsUpdating));
        }

        return new ProductContractDataModificationResponse(productContract.getId(), resigningMessages);
    }

    @Transactional
    public void removeStartAndEndDatesFromPodsByContractDetail(Long productContractDetailId) {
        List<ContractPods> pods = contractPodRepository.findAllByContractDetailIdAndStatusIn(
                productContractDetailId,
                List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(pods)) {
            log.info("No Contract Pods found for Product Contract Detail ID: {}", productContractDetailId);
            return;
        }

        pods.forEach(pod -> {
            pod.setActivationDate(null);
            pod.setDeactivationDate(null);
            pod.setDeactivationPurposeId(null);
            pod.setDealNumber(null);
            log.info("Removed start and end dates for Pod with ID: {}", pod.getId());
        });

        productContractPodService.saveAll(pods);

        log.info("Successfully removed start and end dates from {} Pods associated with Product Contract Detail ID: {}", pods.size(), productContractDetailId);
    }


    private void setProductContractPointOfDeliveryDealManuallyOnUpdate(ProductContractDetails productContractDetails,
                                                                       List<ContractPodRequest> models,
                                                                       List<String> errorMessagesContext) {
        if (productContractDetails.getVersionStatus() != SIGNED) {
            return;
        }
        for (int i = 0; i < models.size(); i++) {
            ContractPodRequest contractPodRequest = models.get(i);
            List<ProductContractPointOfDeliveryRequest> productContractPointOfDeliveriesWithDealNumber = contractPodRequest.getProductContractPointOfDeliveries();

            for (int j = 0; j < productContractPointOfDeliveriesWithDealNumber.size(); j++) {
                ProductContractPointOfDeliveryRequest productContractPointOfDeliveryRequest = productContractPointOfDeliveriesWithDealNumber.get(j);
                Optional<ContractPods> productContractPointOfDeliveryOptional = contractPodRepository
                        .findByContractDetailIdAndPodDetailIdAndStatusIn(
                                productContractDetails.getId(),
                                productContractPointOfDeliveryRequest.pointOfDeliveryDetailId(),
                                List.of(EntityStatus.ACTIVE));
                if (productContractPointOfDeliveryOptional.isEmpty()) {
                    errorMessagesContext.add("podRequests[%s].productContractPointOfDeliveries[%s].pointOfDeliveryDetailId-Product Contract Detail with id: [%s] is not connected to current contract, cannot set deal number;".formatted(i, j, productContractPointOfDeliveryRequest.pointOfDeliveryDetailId()));
                    continue;
                }

                ContractPods productContractPointOfDelivery = productContractPointOfDeliveryOptional.get();
                if (StringUtils.isBlank(productContractPointOfDeliveryRequest.dealNumber())) {
                    productContractPointOfDelivery.setDealNumber(null);
                } else {
                    String pointOfDeliveryDealNumber = productContractPointOfDelivery.getDealNumber();
                    if (!Objects.equals(pointOfDeliveryDealNumber, productContractPointOfDeliveryRequest.dealNumber())) {
                        Optional<PointOfDelivery> pointOfDeliveryOptional = pointOfDeliveryRepository
                                .findByContractPodId(productContractPointOfDelivery.getId());
                        if (pointOfDeliveryOptional.isPresent()) {
                            PointOfDelivery pointOfDelivery = pointOfDeliveryOptional.get();

                            List<String> contractNumbersByPointOfDeliveryDealNumber =
                                    contractPodRepository
                                            .findContractNumbersByPointOfDeliveryDealNumberOrProductContractDealNumber(productContractDetails.getContractId(), pointOfDelivery.getId(), productContractPointOfDeliveryRequest.dealNumber());
                            if (CollectionUtils.isNotEmpty(contractNumbersByPointOfDeliveryDealNumber)) {
                                errorMessagesContext.add("podRequests[%s].productContractPointOfDeliveries[%s].dealNumber-Deal Number is used in other contract: [%s];".formatted(i, j, StringUtils.join(contractNumbersByPointOfDeliveryDealNumber, ",")));
                            }
                        } else {
                            errorMessagesContext.add("podRequests[%s].productContractPointOfDeliveries[%s].dealNumber-Point Of Delivery not found for detail with id: [%s];".formatted(i, j, productContractPointOfDeliveryRequest.pointOfDeliveryDetailId()));
                        }

                        Optional<Customer> customerOptional = customerRepository
                                .findByCustomerDetailIdAndStatusIn(productContractDetails.getCustomerDetailId(), List.of(CustomerStatus.ACTIVE));
                        if (customerOptional.isEmpty()) {
                            errorMessagesContext.add("podRequests[%s].productContractPointOfDeliveries[%s].pointOfDeliveryDetailId-Customer not found for Customer Detail id: [%s];".formatted(i, j, productContractDetails.getCustomerDetailId()));
                            continue;
                        }

                        try {
                            if (!xEnergieRepository.isDealExistsForCustomer(productContractPointOfDeliveryRequest.dealNumber(), customerOptional.get().getIdentifier())) {
                                errorMessagesContext.add("podRequests[%s].productContractPointOfDeliveries[%s].dealNumber-Deal is not valid for this Point Of Delivery;".formatted(i, j));
                            }
                        } catch (Exception e) {
                            errorMessagesContext.add("podRequests[%s].productContractPointOfDeliveries[%s].pointOfDeliveryDetailId-Exception handled while trying to set Product Contract Point Of Delivery deal number manually;".formatted(i, j));
                        }
                    }

                    if (CollectionUtils.isEmpty(errorMessagesContext)) {
                        productContractPointOfDelivery.setDealNumber(productContractPointOfDeliveryRequest.dealNumber());
                    }
                }
            }
        }
    }

    private void setProductContractPointOfDeliveryDealManuallyOnCreate(ProductContractDetails productContractDetails,
                                                                       List<ProductContractPointOfDeliveryRequest> models,
                                                                       List<String> errorMessagesContext) {
        for (int i = 0; i < models.size(); i++) {
            ProductContractPointOfDeliveryRequest productContractPointOfDeliveryRequest = models.get(i);
            if (StringUtils.isBlank(productContractPointOfDeliveryRequest.dealNumber())) {
                continue;
            }

            Optional<ContractPods> productContractPointOfDeliveryOptional = contractPodRepository
                    .findByContractDetailIdAndPodDetailIdAndStatusIn(
                            productContractDetails.getId(),
                            productContractPointOfDeliveryRequest.pointOfDeliveryDetailId(),
                            List.of(EntityStatus.ACTIVE));
            if (productContractPointOfDeliveryOptional.isEmpty()) {
                errorMessagesContext.add("productContractPointOfDeliveries[%s]-Product Contract Detail with id: [%s] is not connected to current contract, cannot set deal number;".formatted(i, productContractPointOfDeliveryRequest.pointOfDeliveryDetailId()));
                continue;
            }

            ContractPods productContractPointOfDelivery = productContractPointOfDeliveryOptional.get();
            String pointOfDeliveryDealNumber = productContractPointOfDelivery.getDealNumber();
            if (!Objects.equals(pointOfDeliveryDealNumber, productContractPointOfDeliveryRequest.dealNumber())) {
                Optional<PointOfDelivery> pointOfDeliveryOptional = pointOfDeliveryRepository
                        .findByContractPodId(productContractPointOfDelivery.getId());
                if (pointOfDeliveryOptional.isPresent()) {
                    PointOfDelivery pointOfDelivery = pointOfDeliveryOptional.get();

                    List<String> contractNumbersByPointOfDeliveryDealNumber =
                            contractPodRepository
                                    .findContractNumbersByPointOfDeliveryDealNumberOrProductContractDealNumber(productContractPointOfDelivery.getId(), pointOfDelivery.getId(), productContractPointOfDeliveryRequest.dealNumber());
                    if (CollectionUtils.isNotEmpty(contractNumbersByPointOfDeliveryDealNumber)) {
                        errorMessagesContext.add("productContractPointOfDeliveries[%s].dealNumber-Deal Number is used in other contract: [%s];".formatted(i, StringUtils.join(contractNumbersByPointOfDeliveryDealNumber, ",")));
                    }
                } else {
                    errorMessagesContext.add("productContractPointOfDeliveries[%s].dealNumber-Point Of Delivery not found for detail with id: [%s];".formatted(i, productContractPointOfDeliveryRequest.pointOfDeliveryDetailId()));
                }

                Optional<Customer> customerOptional = customerRepository
                        .findByCustomerDetailIdAndStatusIn(productContractDetails.getCustomerDetailId(), List.of(CustomerStatus.ACTIVE));
                if (customerOptional.isEmpty()) {
                    errorMessagesContext.add("productContractPointOfDeliveries[%s]-Customer not found for Customer Detail id: [%s];".formatted(i, productContractDetails.getCustomerDetailId()));
                    continue;
                }

                try {
                    if (!xEnergieRepository.isDealExistsForCustomer(productContractPointOfDeliveryRequest.dealNumber(), customerOptional.get().getIdentifier())) {
                        errorMessagesContext.add("productContractPointOfDeliveries[%s]-Deal is not valid for this Point Of Delivery;".formatted(i));
                    }
                } catch (Exception e) {
                    errorMessagesContext.add("productContractPointOfDeliveries[%s]-Exception handled while trying to set Product Contract Point Of Delivery deal number manually;".formatted(i));
                }
            }

            productContractPointOfDelivery.setDealNumber(productContractPointOfDeliveryRequest.dealNumber());
        }
    }

    /**
     * Starts the resigning process for a product contract.
     *
     * @param updateRequest    the update request for the contract
     * @param errorMessages    the list of error messages
     * @param initialSubStatus the initial sub status of the contract details
     * @param productContract  the product contract
     * @param detailsUpdating  the details being updated
     * @param sourceDetails    the source details for the contract
     * @return the list of resigning messages
     */
    private List<String> startResigningProcess(ProductContractUpdateRequest updateRequest, List<String> errorMessages, ContractDetailsSubStatus initialSubStatus, ProductContract productContract, ProductContractDetails detailsUpdating, ProductContractDetails sourceDetails) {
        List<String> resigningMessages = new ArrayList<>();
        if (CollectionUtils.isEmpty(errorMessages) && updateRequest.getBasicParameters().getVersionStatus() == SIGNED) {
            if (!List.of(ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES, ContractDetailsSubStatus.SPECIAL_PROCESSES).contains(initialSubStatus)) {
                if (List.of(ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES, ContractDetailsSubStatus.SPECIAL_PROCESSES).contains(updateRequest.getBasicParameters().getSubStatus())) {
                    try {
                        if (updateRequest.isSavingAsNewVersion()) {
                            productContractResignService.executeResign(updateRequest.getBasicParameters().getSigningDate(), productContract, detailsUpdating, resigningMessages);
                        } else {
                            productContractResignService.executeResign(updateRequest.getBasicParameters().getSigningDate(), productContract, sourceDetails, resigningMessages);
                        }
                    } catch (Exception e) {
                        // for transaction rollback purposes only, no exception handling needed
                    }
                }
            }
        }
        return resigningMessages;
    }

    private List<String> startResigningProcess(ProductContractCreateRequest createRequest, List<String> errorMessages, ProductContract productContract, ProductContractDetails productContractDetails) {
        List<String> resigningMessages = new ArrayList<>();
        if (CollectionUtils.isEmpty(errorMessages) && createRequest.getBasicParameters().getVersionStatus() == SIGNED) {
            if (List.of(ContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES, ContractDetailsSubStatus.SPECIAL_PROCESSES).contains(createRequest.getBasicParameters().getSubStatus())) {
                try {
                    productContractResignService.executeResign(createRequest.getBasicParameters().getSigningDate(), productContract, productContractDetails, resigningMessages);
                } catch (Exception e) {
                    // for transaction rollback purposes only, no exception handling needed
                }
            }
        }
        return resigningMessages;
    }


    private void updateStartDate(Long contractId, ProductContractUpdateRequest updateRequest, ProductContractDetails sourceDetails, List<String> errorMessages) {
        if (updateRequest.isSavingAsNewVersion()) {
            // TODO: 11.10.23 -> need to be implemented later
            //check on version dates is in productContractBasicParametersService

        } else {
            LocalDate requestedStartDate = updateRequest.getStartDate();
            Optional<ProductContractDetails> previousVersionOptional = productContractDetailsRepository
                    .findProductContractPreviousVersion(contractId, sourceDetails.getStartDate(), PageRequest.of(0, 1));

            Optional<ProductContractDetails> nextVersionOptional = productContractDetailsRepository
                    .findProductContractNextVersion(contractId, sourceDetails.getStartDate(), PageRequest.of(0, 1));

            if (previousVersionOptional.isPresent()) {
                ProductContractDetails previousVersion = previousVersionOptional.get();
                if (!requestedStartDate.isAfter(previousVersion.getStartDate())) {
                    errorMessages.add("startDate-Start date must be more then previous version start date;");
                }
            }

            if (nextVersionOptional.isPresent()) {
                ProductContractDetails nextVersion = nextVersionOptional.get();
                if (!requestedStartDate.isBefore(nextVersion.getStartDate())) {
                    errorMessages.add("startDate-Start date must be less then next version start date;");
                }
            }

            if (CollectionUtils.isEmpty(errorMessages)) {
                int comparisonValue = Integer.signum(requestedStartDate.compareTo(sourceDetails.getStartDate()));
                switch (comparisonValue) {
                    case -1 -> {
                        /*
                         * Requested start date is less than old start date
                         */
                        startDateUpdateRouters
                                .stream()
                                .filter(route -> route.getRoute().equals(ProductContractStartDateUpdateRouteTypes.LESS_THEN_OLD_START_DATE))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Cannot find service to execute start date updating"))
                                .recalculateDates(
                                        requestedStartDate,
                                        sourceDetails,
                                        previousVersionOptional.orElse(null),
                                        nextVersionOptional.orElse(null),
                                        errorMessages
                                );
                    }
                    case 0 -> {
                            /*
                            Requested start date equals to old start date
                            */

                            /*
                                Nothing happens here
                             */
                    }
                    case 1 -> {
                            /*
                                Requested start date is more than old start date
                             */
                        startDateUpdateRouters
                                .stream()
                                .filter(route -> route.getRoute().equals(ProductContractStartDateUpdateRouteTypes.MORE_THEN_OLD_START_DATE))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Cannot find service to execute start date updating"))
                                .recalculateDates(
                                        requestedStartDate,
                                        sourceDetails,
                                        previousVersionOptional.orElse(null),
                                        nextVersionOptional.orElse(null),
                                        errorMessages);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + comparisonValue);
                }

                sourceDetails.setStartDate(requestedStartDate);
            }
        }
    }

    private void updateContractStatusAndSendFilesForSignIfApplicable(ProductContract productContract, ProductContractUpdateRequest updateRequest, int versionId, List<String> messages) {
        ProductContractBasicParametersCreateRequest basicParameters = updateRequest.getBasicParameters();
        ContractDetailsStatus newStatus = basicParameters.getStatus();
        LocalDate statusModifyDate = basicParameters.getStatusModifyDate();
        ContractDetailsSubStatus newSubStatus = basicParameters.getSubStatus();
        ContractDetailsStatus oldStatus = productContract.getContractStatus();
        changeStatus(productContract, messages, oldStatus, newStatus, newSubStatus, statusModifyDate);
        productContract.setSigningDate(basicParameters.getSigningDate());
        sendContractFilesForSignIfApplicable(oldStatus, productContract.getId(), versionId, updateRequest);
    }

    private void sendContractFilesForSignIfApplicable(ContractDetailsStatus oldStatus, Long productContractId, int versionId, ProductContractUpdateRequest updateRequest){
        List<ContractDetailsStatus> validStatusesForSigning = List.of(
                ContractDetailsStatus.READY,
                ContractDetailsStatus.SIGNED,
                ContractDetailsStatus.ENTERED_INTO_FORCE,
                ContractDetailsStatus.ACTIVE_IN_TERM,
                ContractDetailsStatus.ACTIVE_IN_PERPETUITY,
                ContractDetailsStatus.CHANGED_WITH_AGREEMENT
        );
        ProductContractBasicParametersCreateRequest basicParameters = updateRequest.getBasicParameters();
        if (!updateRequest.isSavingAsNewVersion() && oldStatus.equals(ContractDetailsStatus.DRAFT) && validStatusesForSigning.contains(basicParameters.getStatus())) {
            Optional<ProductContractDetails> productContractDetails = productContractDetailsRepository.findByContractIdAndVersionId(productContractId, Math.toIntExact(versionId));
            if( productContractDetails.isPresent()){
                List<Document> documentList = signableDocumentRepository.getDocumentsForContractByContractDetailId(productContractDetails.get().getId());
                signerChainManager.startSign(documentList);
            }
        }
    }

    private void changeStatus(ProductContract productContract, List<String> messages, ContractDetailsStatus oldStatus, ContractDetailsStatus newStatus, ContractDetailsSubStatus newSubStatus, LocalDate statusModifyDate) {
        if (!ProductContractStatusChainUtil.canBeChanged(oldStatus, newStatus)) {
            messages.add("basicParameters.status-Status can not be changed to %s".formatted(newStatus));
        }
        productContract.setContractStatus(newStatus);
        if (!productContract.getContractStatus().isCorrectSubStatus(newSubStatus)) {
            messages.add("basicParameters.subStatus-Sub status is incorrect for current status: %s".formatted(newStatus));
        }
        if (!oldStatus.equals(newStatus) || !newSubStatus.equals(productContract.getSubStatus())) {
            if (statusModifyDate != null) {
                productContract.setStatusModifyDate(statusModifyDate);
            } else {
                productContract.setStatusModifyDate(LocalDate.now());
            }
        } else if (statusModifyDate != null) {
            productContract.setStatusModifyDate(statusModifyDate);
        }
        if (oldStatus.equals(ContractDetailsStatus.TERMINATED) && !oldStatus.equals(newStatus)) {
            productContract.setTerminationDate(null);
        }
        productContract.setSubStatus(newSubStatus);
    }

    private List<ProductContractStatus> getCustomerStatuses() {
        return List.of(ProductContractStatus.ACTIVE);
    }

    public Page<ProductContractListingResponse> list(ProductContractListingRequest request) {
        return productContractRepository
                .filter(
                        request.getPrompt() == null ? null : EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        extractSearchBy(request.getSearchBy()),
                        request.getActivationFrom(),
                        request.getActivationTo(),
                        request.getDateOfEntryIntoPerpetuityFrom(),
                        request.getDateOfEntryIntoPerpetuityTo(),
                        request.getDateOfTerminationFrom(),
                        request.getDateOfTerminationTo(),
                        CollectionUtils.isEmpty(request.getContractDetailsStatuses()) ? null : request.getContractDetailsStatuses(),
                        CollectionUtils.isEmpty(request.getContractDetailsSubStatuses()) ? null : request.getContractDetailsSubStatuses(),
                        CollectionUtils.isEmpty(request.getTypes()) ? null : request.getTypes(),
                        CollectionUtils.isEmpty(request.getProductIds()) ? null : request.getProductIds(),
                        CollectionUtils.isEmpty(request.getAccountManagerIds()) ? null : request.getAccountManagerIds(),
                        ExcludeVersions.getExcludeVersionFromCheckBoxes(request.isExcludeOldVersions(), request.isExcludeFutureVersions()).getValue(),
                        getStatuses(),
                        PageRequest.of(request.getPage(), request.getSize(), extractSorting(request))
                );
    }

    private List<ProductContractStatus> getStatuses() {
        List<ProductContractStatus> statuses = new ArrayList<>();

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCT_CONTRACTS, List.of(PermissionEnum.PRODUCT_CONTRACT_VIEW))) {
            statuses.add(ProductContractStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRODUCT_CONTRACTS, List.of(PermissionEnum.PRODUCT_CONTRACT_VIEW_DELETED))) {
            statuses.add(ProductContractStatus.DELETED);
        }

        return statuses;
    }

    private String extractSearchBy(ProductContractListingSearchFields searchFields) {
        return Objects.requireNonNullElse(searchFields, ProductContractListingSearchFields.ALL).name();
    }

    private Sort extractSorting(ProductContractListingRequest request) {
        return Sort.by(
                Objects.requireNonNullElse(request.getDirection(), Sort.Direction.ASC),
                Objects.requireNonNullElse(request.getSortBy(), ProductContractListingSortFields.ID).getValue()
        );
    }

    public Page<ProductContractProductListingResponse> productListingForContract(ProductContractProductListingRequest request) {
        return basicParametersService.getProducts(request).map(ProductContractProductListingResponse::new);
    }

    public ProductContractThirdPageFields thirdTabFields(Long productDetailId) {
        return productParametersService.thirdTabFields(productDetailId);
    }


    /**
     * Retrieves product contract details by contract id and version id.
     * If version id is not provided, the latest version is returned.
     *
     * @param id        contract id to retrieve details for
     * @param versionId version id to retrieve details for
     * @return {@link ProductContractResponse} object
     */
    public ProductContractResponse get(Long id, Integer versionId) {
        log.debug("Retrieving product contract details for contract id: {}, version id: {}", id, versionId);
        ProductContractResponse response = new ProductContractResponse();
        ProductContract productContract = productContractRepository
                .findByIdAndStatusIn(id, getStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Product contract not found!"));
        response.setStatus(productContract.getStatus());
        response.setLocked(productContract.getLocked() == null ? Boolean.FALSE : productContract.getLocked());
        ProductContractDetails details;
        if (versionId != null) {
            // TODO: 7/20/23 do we need to fetch by any specific statuses?
            details = productContractDetailsRepository
                    .findByContractIdAndVersionId(id, versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Product contract details not found"));
        } else {
            details = productContractDetailsRepository
                    .findFirstByContractIdOrderByStartDateDesc(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Product contract details not found"));
        }

        // additional parameters tab full response
        response.setBasicParameters(basicParametersService.getBasicParameterResponse(productContract, details));
        response.setAdditionalParameters(additionalParametersService.getAdditionalParametersResponse(details));
        ProductContractThirdPageFields thirdPageTabs = productParametersService.thirdTabFields(details.getProductDetailId());
        response.setThirdPageTabs(thirdPageTabs);
        response.setProductParameters(productParametersService.thirdPagePreview(details, productContract, thirdPageTabs));
        response.setContractPodsResponses(productContractPodService.getAllPodsByCustomerDetailId(details.getId()));
        response.setBillingGroups(billingGroupService.findContractBillingGroups(productContract.getId()));
        response.setVersions(
                productContractDetailsRepository.findProductContractVersionsOrderedByStatusAndStartDate(
                        productContract.getId()
                )
        );
        response.setLockedByInvoice(isLockedByInvoice(id));

        // TODO: 7/20/23 implement fully later
        return response;
    }

    private boolean isLockedByInvoice(Long contractId) {
        return invoiceRepository.existsInvoiceByInvoiceStatusAndProductContractId(InvoiceStatus.REAL, contractId);
    }


    /**
     * Deletes a product contract by id if all conditions are met.
     *
     * @param id id of the product contract to delete
     * @return id of the deleted product contract
     */
    public Long delete(Long id) {
        log.debug("Deleting product contract with id: {}", id);

        ProductContract productContract = productContractRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Product contract not found by ID %s;".formatted(id)));

        if (productContract.getStatus().equals(ProductContractStatus.DELETED)) {
            log.error("Product contract with id: {} is already deleted", id);
            throw new OperationNotAllowedException("Product contract is already deleted");
        }

        ContractDetailsStatus contractStatus = productContract.getContractStatus();
        if (!List.of(ContractDetailsStatus.READY, ContractDetailsStatus.DRAFT).contains(contractStatus)) {
            log.error("You can delete contract with status [READY, DRAFT], current contract status is [%s]".formatted(contractStatus));
            throw new OperationNotAllowedException("You can delete contract with status [READY, DRAFT], current contract status is [%s]".formatted(contractStatus));
        }

        if (isLockedByInvoice(id)) {
            throw new OperationNotAllowedException("Can't delete because it is connected to invoice");
        }

        if (productContractRepository.hasConnectionToActivity(id)) {
            log.debug("Product contract with id: {} has connection to activity", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is connected to an activity.");
        }

        if (productContractRepository.hasConnectionToProductContract(id)) {
            log.debug("Product contract with id: {} has connection to Product Contract", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is connected to another Product Contract.");
        }

        if (productContractRepository.hasConnectionToServiceContract(id) || productContractRepository.isLinkedToServiceContract(id)) {
            log.debug("Product contract with id: {} has connection to Service Contract", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is connected to Service Contract.");
        }

        if (productContractRepository.hasConnectionToServiceOrder(id)) {
            log.debug("Product contract with id: {} has connection to Service Order", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is connected to Service Order.");
        }
        if (productContractRepository.isLinkedToServiceOrder(id)) {
            log.debug("Product contract with id: {} is linked Service Order", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is linked to Service Order.");

        }
        if (productContractRepository.hasConnectionToGoodsOrder(id)) {
            log.debug("Product contract with id: {} has connection to Goods Order", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is connected to Goods Order.");
        }

        if (productContractRepository.hasConnectionToTask(id)) {
            log.debug("Product contract with id: {} has connection to Task", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is connected to Task.");
        }

        if (productContractRepository.hasConnectionToAction(id)) {
            log.debug("Product contract with id: {} has connection to Action", id);
            throw new OperationNotAllowedException("You cannot delete product contract because it is connected to Action.");
        }

        productContract.setStatus(ProductContractStatus.DELETED);
        productContractRepository.save(productContract);

        return id;
    }


    public List<ContractPodsResponse> getAllPods(Long customerId) {
        return productContractPodService.getAllPods(customerId);
    }

    public List<ContractPodsResponse> getActivePods(Long customerId, Long contractId) {
        return productContractPodService.getActivePods(customerId, contractId);
    }

    public byte[] getTemplate() {
        return productContractPodService.getTemplate();
    }

    public List<ContractPodsResponse> importFile(MultipartFile file, Long customerDetailId, Long contractId) {
        return productContractPodService.importFile(file, customerDetailId, contractId);
    }

    public List<ContractPodsResponse> getConcretePodWithVersions(String identifier) {
        return productContractPodService.getConcretePodWithVersions(identifier);
    }

    public void calculateAdditionalAgreementSuffix(ProductContractDetails contractDetails, ProductContract contract) {
        Optional<Integer> contractAgreementSuffixValue = productContractDetailsRepository.findContractAgreementSuffixValue(contract.getId());
        if ((contractDetails.getType().equals(ContractDetailType.ADDITIONAL_AGREEMENT) || contractDetails.getType().equals(ContractDetailType.EX_OFFICIO_AGREEMENT)) && contractDetails.getAgreementSuffix() == null) {
            if (contractAgreementSuffixValue.isEmpty()) {
                contractDetails.setAgreementSuffix(1);
            } else {
                Integer suffix = contractAgreementSuffixValue.get();
                contractDetails.setAgreementSuffix(suffix + 1);
            }
        }
    }


    /**
     * Retrieves all activities for a product contract.
     *
     * @param id product contract id
     * @return list of {@link SystemActivityShortResponse} objects
     */
    public List<SystemActivityShortResponse> getActivitiesById(Long id) {
        return productContractActivityService.getActivitiesByConnectedObjectId(id);
    }

    public List<TaskShortResponse> getTasksById(Long id) {
        return taskService.getTasksByProductContractId(id);
    }

    @ExecutionTimeLogger
    @Transactional
    public List<ProductContract> updateProductContractsFromSchedulerJob() {
        LocalDate nowDate = LocalDate.now();
        List<ProductContract> productContracts = productContractRepository.getProductContractsForStatusUpdateFromJob(nowDate);
        List<ProductContract> contractsToUpdate = new ArrayList<>();
        productContracts.forEach(pc -> {
            try {
                LocalDate activationDate = pc.getActivationDate();
                LocalDate entryIntoForceDate = pc.getEntryIntoForceDate();
                if ((activationDate == null && entryIntoForceDate != null && (entryIntoForceDate.isBefore(nowDate) || entryIntoForceDate.equals(nowDate))) ||
                        (activationDate != null && (activationDate.isBefore(nowDate) || activationDate.equals(nowDate)))) {
                    pc.setContractStatus((activationDate == null) ? ContractDetailsStatus.ENTERED_INTO_FORCE : ContractDetailsStatus.ACTIVE_IN_TERM);
                    pc.setSubStatus((activationDate == null) ? ContractDetailsSubStatus.AWAITING_ACTIVATION : ContractDetailsSubStatus.DELIVERY);
                    contractsToUpdate.add(pc);
                }
            } catch (Exception e) {
                log.error("Some error happened when working to update product contract status  with productContractId: %s, skipping current product contract".formatted(pc.getId()), e);
            }
        });
        productContractRepository.saveAllAndFlush(contractsToUpdate);
        contractsToUpdate.forEach(c -> {
            List<Long> customerIdsToChangeStatusWithContractId = productContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(c.getId());
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);
        });
        return contractsToUpdate;
    }

    @Transactional
    public Long updateStatus(ProductContractEditStatusRequest request, Long id, Integer versionId) {

        ProductContract productContract = productContractRepository.findByIdAndStatusIn(id, List.of(ProductContractStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Contract can't be found;"));
        List<String> errorMessages = new ArrayList<>();

        changeStatus(productContract, errorMessages, request.getContractStatus(), productContract.getContractStatus(), request.getContractSubStatus(), LocalDate.now());
        ProductContractDetails productContractDetails = productContractDetailsRepository.findByContractIdAndVersionId(id, versionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("versionId-version not found!;"));
        productContractDetails.setVersionStatus(request.getContractVersionStatus());
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        productContractRepository.save(productContract);
        productContractDetailsRepository.save(productContractDetails);
        return productContract.getId();
    }


}
