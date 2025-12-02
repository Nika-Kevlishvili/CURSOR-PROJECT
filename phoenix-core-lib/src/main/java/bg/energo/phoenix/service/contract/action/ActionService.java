package bg.energo.phoenix.service.contract.action;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.Action;
import bg.energo.phoenix.model.entity.contract.action.ActionFile;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.enums.contract.action.ActionSearchField;
import bg.energo.phoenix.model.enums.contract.action.ActionStatus;
import bg.energo.phoenix.model.enums.contract.action.ActionTableColumn;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.contract.action.*;
import bg.energo.phoenix.model.response.contract.action.*;
import bg.energo.phoenix.model.response.contract.action.calculation.ActionPenaltyCalculationResult;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.contract.action.ActionFileRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationNotificationChannelsRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.contract.action.calculation.ActionPenaltyCalculationService;
import bg.energo.phoenix.service.contract.action.document.ActionDocumentCreationService;
import bg.energo.phoenix.service.contract.action.file.ActionFileService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.util.StringUtil;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.contract.action.ActionTypeProperties;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer.CUSTOMER;
import static bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer.EPRES;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ActionFileRepository actionFileRepository;
    private final ActionValidatorService actionValidatorService;
    private final ActionPodService actionPodService;
    private final ActionRepository actionRepository;
    private final ActionFileService actionFileService;
    private final PermissionService permissionService;
    private final CustomerRepository customerRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final PointOfDeliveryRepository podRepository;
    private final PenaltyRepository penaltyRepository;
    private final TerminationRepository terminationRepository;
    private final ActionTypeProperties actionTypeProperties;
    private final ActionPenaltyCalculationService penaltyCalculationService;
    private final CustomerLiabilityService customerLiabilityService;
    private final TerminationNotificationChannelsRepository terminationNotificationChannelsRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationService;
    private final ActionDocumentCreationService actionDocumentCreationService;

    /**
     * Creates an action for a contract be able to calculate contracts or point of deliveries penalty amount
     * and create liability automatically.
     *
     * @param request {@link ActionRequest} containing the data for the action to be created
     * @return ID of the created action
     */
    @Transactional
    public ActionResponseWithInfoErrorMessages create(ActionRequest request) {
        log.debug("Creating action with request: {}", request);
        List<String> errorMessages = new ArrayList<>();

        actionValidatorService.validateRequest(request, null, List.of(NomenclatureItemStatus.ACTIVE), List.of(NomenclatureItemStatus.ACTIVE), errorMessages);
        ActionPenaltyCalculationResult actionPenaltyCalculationResult = penaltyCalculationService.calculatePenaltyAmount(request, null);
        Action action = ActionMapper.fromRequestToEntity(new Action(), request, actionPenaltyCalculationResult);
        validateAndSetTemplate(request.getEmailTemplateId(), null, ContractTemplateType.EMAIL, errorMessages);
        validateAndSetTemplate(request.getTemplateId(), null, ContractTemplateType.DOCUMENT, errorMessages);
        actionRepository.saveAndFlush(action);
        actionPodService.createActionPods(request.getPods(), action.getId());
        // TODO: 10/26/23 create communications (will be implemented in the following bundles)
        actionFileService.attachFilesToAction(request.getFiles(), action.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        if ((action.getExecutionDate().isEqual(LocalDate.now()) || action.getExecutionDate().isBefore(LocalDate.now())) && isLiabilityGenerationApplicable(request.getPenaltyPayer(),
                request.getDontAllowAutomaticPenaltyClaim(), actionPenaltyCalculationResult, action.getClaimAmountManuallyEntered())) {
            customerLiabilityService.createLiabilityFromAction(action.getId());

            if (customerLiabilityService.isLiabilityGeneratedForAction(action.getId())) {
                actionDocumentCreationService.generateActionPenaltyDocumentAndSendEmail(action.getId());
            }
        }

        updateContractSubStatus(request);
        archiveFiles(action);
        return ActionResponseWithInfoErrorMessages.of(
                action.getId(),
                actionPenaltyCalculationResult.infoErrorMessages()
        );
    }

    private void archiveFiles(Action action) {
        List<ActionFile> actionFiles = actionFileRepository.findByActionIdAndStatusIn(action.getId(), List.of(EntityStatus.ACTIVE));

        Optional<Customer> customerOptional = Optional.empty();
        if (Objects.nonNull(action.getCustomerId())) {
            customerOptional = customerRepository.findById(action.getCustomerId());
        }

        if (CollectionUtils.isNotEmpty(actionFiles)) {
            for (ActionFile actionFile : actionFiles) {
                try {
                    actionFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_ACTION_FILE);
                    actionFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_ACTION_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), "%s/%s".formatted("Action", action.getId())),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), customerOptional.map(Customer::getIdentifier).orElse("")),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), customerOptional.isPresent() ? customerOptional.get().getCustomerNumber() : ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );
                    actionFile.setNeedArchive(true);

                    fileArchivationService.archive(actionFile);
                } catch (Exception e) {
                    log.error("Cannot archive file: ", e);
                }
            }
        }
    }

    /**
     * Updates contract's sub status based on the action type, contract information and penalty payer.
     *
     * @param request {@link ActionRequest} containing the data for the action
     */
    private void updateContractSubStatus(ActionRequest request) {
        if (actionTypeProperties.isActionTypeRelatedToContractTermination(request.getActionTypeId())) {
            updateContractSubStatusWhenActionTypeContractTermination(request);
        } else if (actionTypeProperties.isActionTypeRelatedToPodTermination(request.getActionTypeId())) {
            updateProductContractSubStatusWhenActionTypePodTerminationWithoutNotice(request);
        }
    }


    /**
     * Updates contract sub status when action type is related to contract termination.
     * This flow handles both, product and service contracts.
     *
     * @param request {@link ActionRequest} containing the data for the action
     */
    private void updateContractSubStatusWhenActionTypeContractTermination(ActionRequest request) {
        switch (request.getContractType()) {
            case PRODUCT_CONTRACT -> updateProductContractSubStatusWhenActionTypeContractTermination(request);
            case SERVICE_CONTRACT -> updateServiceContractSubStatusWhenActionTypeContractTermination(request);
        }
    }


    /**
     * Handles product contract sub status update when action type is related to contract termination.
     * If contract status is active in term or active in perpetuity and sub status is delivery or in termination by GO data,
     * then sub status should be updated to in termination by customer or in termination by EPRES, according to the penalty payer.
     * If contract status is in termination by customer or in termination by EPRES and there is a mismatch between the penalty payer
     * of the newly created action and the sub status of the contract, update the sub status considering other action states.
     *
     * @param request {@link ActionRequest} containing the data for the action
     */
    private void updateProductContractSubStatusWhenActionTypeContractTermination(ActionRequest request) {
        log.debug("Updating product contract sub status when action type is related to contract termination;");

        ProductContract productContract = productContractRepository
                .findByIdAndStatusIn(request.getContractId(), List.of(ProductContractStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("contractId-Product contract not found by ID %s;".formatted(request.getContractId())));

        ContractDetailsStatus contractStatus = productContract.getContractStatus();
        ContractDetailsSubStatus contractSubStatus = productContract.getSubStatus();
        ActionPenaltyPayer penaltyPayer = request.getPenaltyPayer();

        if (List.of(ContractDetailsStatus.ACTIVE_IN_TERM, ContractDetailsStatus.ACTIVE_IN_PERPETUITY).contains(contractStatus)) {
            if (List.of(ContractDetailsSubStatus.DELIVERY, ContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA).contains(contractSubStatus)) {
                switch (penaltyPayer) {
                    case CUSTOMER -> productContract.setSubStatus(ContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER);
                    case EPRES -> productContract.setSubStatus(ContractDetailsSubStatus.IN_TERMINATION_BY_EPRES);
                }
            } else if (List.of(ContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER, ContractDetailsSubStatus.IN_TERMINATION_BY_EPRES).contains(contractSubStatus)) {
                if ((contractSubStatus.equals(ContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER) && !penaltyPayer.equals(CUSTOMER))
                        || (contractSubStatus.equals(ContractDetailsSubStatus.IN_TERMINATION_BY_EPRES) && !penaltyPayer.equals(EPRES))) {

                    List<Action> persistedActionsWithSameContractAndActionType = actionRepository.findByContractAndActionType(
                            productContract.getId(),
                            null,
                            request.getActionTypeId(),
                            List.of(EntityStatus.ACTIVE)
                    );

                    if (CollectionUtils.isEmpty(persistedActionsWithSameContractAndActionType)) {
                        setProductContractSubStatusBasedOnPenaltyPayer(productContract, penaltyPayer);
                    } else {
                        Action earliestPersistedAction = persistedActionsWithSameContractAndActionType.get(0);
                        setProductContractSubStatusBasedOnPenaltyPayer(
                                productContract,
                                request.getExecutionDate().isBefore(earliestPersistedAction.getExecutionDate())
                                        ? penaltyPayer
                                        : earliestPersistedAction.getPenaltyPayer()
                        );
                    }
                }
            }

            productContractRepository.save(productContract);
        }
    }


    /**
     * Sets product contract sub status according to the penalty payer.
     *
     * @param productContract {@link ProductContract} to be updated
     * @param penaltyPayer    {@link ActionPenaltyPayer} to be used for the update
     */
    private void setProductContractSubStatusBasedOnPenaltyPayer(ProductContract productContract, ActionPenaltyPayer penaltyPayer) {
        switch (penaltyPayer) {
            case CUSTOMER -> productContract.setSubStatus(ContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER);
            case EPRES -> productContract.setSubStatus(ContractDetailsSubStatus.IN_TERMINATION_BY_EPRES);
        }
    }


    /**
     * Handles service contract sub status update when action type is related to contract termination.
     * If contract status is active in term or active in perpetuity and sub status is delivery or in termination by GO data,
     * then sub status should be updated to in termination by customer or in termination by EPRES, according to the penalty payer.
     * If contract status is in termination by customer or in termination by EPRES and there is a mismatch between the penalty payer
     * of the newly created action and the sub status of the contract, update the sub status considering other action states.
     *
     * @param request {@link ActionRequest} containing the data for the action
     */
    private void updateServiceContractSubStatusWhenActionTypeContractTermination(ActionRequest request) {
        log.debug("Updating service contract sub status when action type is related to contract termination;");

        ServiceContracts serviceContract = serviceContractsRepository
                .findByIdAndStatusIn(request.getContractId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("contractId-Service contract not found by ID %s;".formatted(request.getContractId())));

        ServiceContractDetailStatus contractStatus = serviceContract.getContractStatus();
        ServiceContractDetailsSubStatus contractSubStatus = serviceContract.getSubStatus();
        ActionPenaltyPayer penaltyPayer = request.getPenaltyPayer();

        if (List.of(ServiceContractDetailStatus.ACTIVE_IN_TERM, ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY).contains(contractStatus)) {
            if (List.of(ServiceContractDetailsSubStatus.DELIVERY, ServiceContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA).contains(contractSubStatus)) {
                switch (penaltyPayer) {
                    case CUSTOMER ->
                            serviceContract.setSubStatus(ServiceContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER);
                    case EPRES -> serviceContract.setSubStatus(ServiceContractDetailsSubStatus.IN_TERMINATION_BY_EPRES);
                }
            } else if (List.of(ServiceContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER, ServiceContractDetailsSubStatus.IN_TERMINATION_BY_EPRES).contains(contractSubStatus)) {
                if ((contractSubStatus.equals(ServiceContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER) && !penaltyPayer.equals(CUSTOMER))
                        || contractSubStatus.equals(ServiceContractDetailsSubStatus.IN_TERMINATION_BY_EPRES) && !penaltyPayer.equals(EPRES)) {

                    List<Action> persistedActionsWithTheSameContractAndActionType = actionRepository.findByContractAndActionType(
                            null,
                            serviceContract.getId(),
                            request.getActionTypeId(),
                            List.of(EntityStatus.ACTIVE)
                    );

                    if (CollectionUtils.isEmpty(persistedActionsWithTheSameContractAndActionType)) {
                        setServiceContractSubStatusBasedOnPenaltyPayer(serviceContract, penaltyPayer);
                    } else {
                        Action earliestPersistedAction = persistedActionsWithTheSameContractAndActionType.get(0);
                        setServiceContractSubStatusBasedOnPenaltyPayer(
                                serviceContract,
                                request.getExecutionDate().isBefore(earliestPersistedAction.getExecutionDate())
                                        ? penaltyPayer
                                        : earliestPersistedAction.getPenaltyPayer()
                        );
                    }
                }
            }

            serviceContractsRepository.save(serviceContract);
        }
    }


    /**
     * Sets service contract sub status according to the penalty payer.
     *
     * @param serviceContract {@link ServiceContracts} to be updated
     * @param penaltyPayer    {@link ActionPenaltyPayer} to be used for the update
     */
    private void setServiceContractSubStatusBasedOnPenaltyPayer(ServiceContracts serviceContract, ActionPenaltyPayer penaltyPayer) {
        switch (penaltyPayer) {
            case CUSTOMER -> serviceContract.setSubStatus(ServiceContractDetailsSubStatus.IN_TERMINATION_BY_CUSTOMER);
            case EPRES -> serviceContract.setSubStatus(ServiceContractDetailsSubStatus.IN_TERMINATION_BY_EPRES);
        }
    }


    /**
     * Updates product contract sub status when action type is related to pod termination without notice.
     * This flow naturally handles only product contracts as service contracts are incompatible with this action type.
     * We are not taking any action when the type is pod termination with notice (such cases will be manually handled).
     * If contract status is active in term or active in perpetuity and sub status is delivery,
     * if all contract pods are covered by actions with the same type and without notice (either with the newly created action,
     * or other persisted actions, or their any kind of combination), then sub status should be updated.
     *
     * @param request {@link ActionRequest} containing the data for the action
     */
    private void updateProductContractSubStatusWhenActionTypePodTerminationWithoutNotice(ActionRequest request) {
        log.debug("Updating product contract sub status when action type is related to pod termination without notice;");

        if (request.getActionTypeId().equals(actionTypeProperties.getPodTerminationWithoutNoticeId())) {
            if (request.getContractType().equals(ContractType.PRODUCT_CONTRACT)) {
                ProductContract productContract = productContractRepository
                        .findById(request.getContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Product not found"));

                ContractDetailsStatus contractStatus = productContract.getContractStatus();
                if (List.of(ContractDetailsStatus.ACTIVE_IN_TERM, ContractDetailsStatus.ACTIVE_IN_PERPETUITY).contains(contractStatus)) {
                    if (Objects.equals(ContractDetailsSubStatus.DELIVERY, productContract.getSubStatus())) {
                        if (actionRepository.allContractPodsAreCoveredByActions(productContract.getId(), request.getActionTypeId(), request.getPods())) {
                            productContract.setSubStatus(ContractDetailsSubStatus.IN_TERMINATION_BY_GO_DATA);
                            productContractRepository.save(productContract);
                        }
                    }
                }
            }
        }
    }


    /**
     * Retrieves an action by the given ID.
     *
     * @param id ID of the action to be retrieved
     * @return {@link ActionResponse} containing the data of the retrieved action
     */
    public ActionResponse view(Long id) {
        log.debug("Viewing action with id: {}", id);

        // TODO: 10/12/23 liability generation field in response should be implemented in the following bundles

        Action action = actionRepository
                .findByIdAndStatusIn(id, getStatusesByPermissions())
                .orElseThrow(() -> new DomainEntityNotFoundException("Action not found by ID %s;".formatted(id)));

        ActionResponse actionResponse = actionRepository.getActionResponse(action.getId());
        actionResponse.setLiabilityGenerated(customerLiabilityService.isLiabilityGeneratedForAction(action.getId()));
        actionResponse.setTerminationNotificationChannels(terminationNotificationChannelsRepository.getTerminationNotificationChannelsByTerminationId(action.getTerminationId()));
        actionResponse.setPods(actionPodService.fetchPodsShortResponses(action.getId()));
        actionResponse.setFiles(actionFileService.getFiles(action.getId()));
        actionResponse.setLiability(customerLiabilityService.getActionLiability(action.getId()));
        if (action.getEmailTemplateId() != null) {
            contractTemplateRepository.findTemplateResponseById(action.getEmailTemplateId(), LocalDate.now())
                    .ifPresent(actionResponse::setEmailTemplateResponse);
        }
        if (action.getTemplateId() != null) {
            contractTemplateRepository.findTemplateResponseById(action.getTemplateId(), LocalDate.now())
                    .ifPresent(actionResponse::setTemplateResponse);
        }
        return actionResponse;
    }


    /**
     * Updates an action if validations pass. A special permission is required and updatable fields are limited if
     * an action's status is EXECUTED or liability is already generated.
     *
     * @param id      ID of the action to be updated
     * @param request {@link ActionRequest} containing the data for the action to be updated
     * @return ID of the updated action
     */
    @Transactional
    public ActionResponseWithInfoErrorMessages update(Long id, ActionRequest request) {
        log.debug("Updating action with id: {} and request: {}", id, request);
        ArrayList<String> errorMessages = new ArrayList<>();

        Action action = actionRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Action not found by ID %s;".formatted(id)));

        List<String> infoErrorMessages;

        boolean liabilityGeneratedForAction = customerLiabilityService.isLiabilityGeneratedForAction(action.getId());

        if (liabilityGeneratedForAction) {
            infoErrorMessages = updateActionWhenLiabilityGenerated(id, request, action, errorMessages);
        } else {
            validateAndSetTemplate(request.getEmailTemplateId(), action, ContractTemplateType.EMAIL, errorMessages);
            validateAndSetTemplate(request.getTemplateId(), action, ContractTemplateType.DOCUMENT, errorMessages);
            action.setTemplateId(request.getTemplateId());
            action.setEmailTemplateId(request.getEmailTemplateId());

            if (action.getActionStatus().equals(ActionStatus.EXECUTED)) {
                infoErrorMessages = updateLockedActionWithSpecialPermission(id, request, action, errorMessages);
            } else {
                infoErrorMessages = updateActionWithStandardPermission(id, request, action, errorMessages);
            }
        }

        archiveFiles(action);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return ActionResponseWithInfoErrorMessages.of(action.getId(), infoErrorMessages);
    }


    /**
     * Updates an action if it is locked (liability generated or/and status executed).
     * When both conditions are true, only additional information, communications and files can be updated.
     *
     * @param id            ID of the action to be updated
     * @param request       {@link ActionRequest} containing the data for the action to be updated
     * @param action        Action to be updated
     * @param errorMessages List of error messages to be populated if validation fails
     * @return List of info error messages collected during the penalty calculation
     */
    private List<String> updateLockedActionWithSpecialPermission(Long id, ActionRequest request, Action action, ArrayList<String> errorMessages) {
        if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.ACTIONS, List.of(PermissionEnum.ACTIONS_EDIT_LOCKED))) {
            log.error("You are not allowed to edit a locked action;");
            throw new AccessDeniedException("You are not allowed to edit a locked action;");
        }

        List<String> infoErrorMessages = new ArrayList<>();


        /*if (action.getActionStatus().equals(ActionStatus.EXECUTED) && liabilityGeneratedForAction) {
            actionValidatorService.validateUnmodifiableFieldsWhenLiabilityGeneratedAndStatusExecuted(request, action);
        } else*/
        //if (action.getActionStatus().equals(ActionStatus.EXECUTED)) {
        infoErrorMessages = updateActionWhenStatusExecuted(request, action, errorMessages);
        /*}
        else if (liabilityGeneratedForAction) {
            updateActionWhenLiabilityGenerated(request, action, errorMessages);
        }*/

        action.setAdditionalInfo(request.getAdditionalInformation());
        // TODO: 10/26/23 update communications (will be implemented in the following bundles)
        actionFileService.updateFiles(request.getFiles(), id, errorMessages);

        return infoErrorMessages;
    }

    /**
     * Updates the specified action when a liability is generated. This method validates the unmodifiable
     * fields, updates action details based on the request, and handles file updates. Any errors encountered
     * during the operation are added to the provided error messages list, while additional informational
     * or error messages are returned.
     *
     * @param id            The unique identifier of the action to be updated.
     * @param request       The action request containing updated information, additional details, and files.
     * @param action        The action object that needs to be updated based on the request.
     * @param errorMessages A list to which error messages encountered during the operation will be added.
     * @return A list of informational or additional error messages gathered during the update process.
     */
    private List<String> updateActionWhenLiabilityGenerated(Long id, ActionRequest request, Action action, ArrayList<String> errorMessages) {
        List<String> infoErrorMessages = new ArrayList<>();

        actionValidatorService.validateUnmodifiableFieldsWhenLiabilityGenerated(request, action);

        action.setAdditionalInfo(request.getAdditionalInformation());
        actionFileService.updateFiles(request.getFiles(), id, errorMessages);

        return infoErrorMessages;
    }


    /**
     * Updates action when its status is executed and liability is not generated.
     * Updatable fields are penalty, penalty claim amount, currency, [don't allow auto penalty claim] checkbox.
     * Penalty should be recalculated and automatic liability generation should be tried (if applicable)
     *
     * @param request       {@link ActionRequest} containing the data for the action to be updated
     * @param action        Action to be updated
     * @param errorMessages List of error messages to be populated if validation fails
     * @return List of info error messages collected during the penalty calculation
     */
    private List<String> updateActionWhenStatusExecuted(ActionRequest request, Action action, ArrayList<String> errorMessages) {
        actionValidatorService.validateUnmodifiableFieldsWhenStatusIsExecuted(request, action);
        actionValidatorService.validatePenalty(request, errorMessages);
        ActionPenaltyCalculationResult actionPenaltyCalculationResult = penaltyCalculationService.calculatePenaltyAmount(request, action.getId());

        action.setPenaltyId(request.getPenaltyId());
        action.setWithoutPenalty(request.getWithoutPenalty());
        BigDecimal amount = actionPenaltyCalculationResult.amount().stripTrailingZeros().scale() <= 0 ?
                actionPenaltyCalculationResult.amount().setScale(0, RoundingMode.HALF_UP)
                : actionPenaltyCalculationResult.amount();
        action.setCalculatedPenaltyAmount(amount);
        action.setCalculatedPenaltyCurrencyId(actionPenaltyCalculationResult.currencyId());
        action.setDontAllowAutomaticPenaltyClaim(request.getDontAllowAutomaticPenaltyClaim());
        if (request.getPenaltyClaimAmount() == null) {
            action.setPenaltyClaimAmount(amount);
            action.setPenaltyClaimCurrencyId(actionPenaltyCalculationResult.currencyId());
        } else {
            action.setPenaltyClaimAmount(request.getPenaltyClaimAmount());
            action.setPenaltyClaimCurrencyId(request.getPenaltyClaimAmountCurrencyId());
        }

        if (isLiabilityGenerationApplicable(request.getPenaltyPayer(),
                request.getDontAllowAutomaticPenaltyClaim(), actionPenaltyCalculationResult, action.getClaimAmountManuallyEntered())) {
            customerLiabilityService.createLiabilityFromAction(action.getId());
        }

        return actionPenaltyCalculationResult.infoErrorMessages();
    }


    /**
     * Updates action when its liability is generated and status is not executed.
     * Updatable fields are notice receiving date and termination.
     *
     * @param request       {@link ActionRequest} containing the data for the action to be updated
     * @param action        Action to be updated
     * @param errorMessages List of error messages to be populated if validation fails
     */
    private void updateActionWhenLiabilityGenerated(ActionRequest request, Action action, ArrayList<String> errorMessages) {
        actionValidatorService.validateUnmodifiableFieldsWhenLiabilityGenerated(request, action);
        if (actionTypeProperties.isActionTypeRelatedToContractTermination(action.getActionTypeId())) {
            actionValidatorService.validateTermination(request, errorMessages);
            action.setTerminationId(request.getTerminationId());
            action.setWithoutAutomaticTermination(request.getWithoutAutomaticTermination());
        } else {
            log.error("Cannot edit Termination when edit type is not contract termination with/without notes;");
            errorMessages.add("Cannot edit Termination when edit type is not contract termination with/without notes;");
        }
        if (action.getExecutionDate().isBefore(request.getNoticeReceivingDate())) {
            log.error("Notice Receiving Date must be less than or equal to the Execution Date");
            errorMessages.add("Notice Receiving Date must be less than or equal to the Execution Date");
        } else {
            action.setNoticeReceivingDate(request.getNoticeReceivingDate());
        }
    }


    /**
     * Updates an action if it is not locked (liability not generated or status not executed).
     *
     * @param id            ID of the action to be updated
     * @param request       {@link ActionRequest} containing the data for the action to be updated
     * @param action        Action to be updated
     * @param errorMessages List of error messages to be populated if validation fails
     * @return List of info error messages collected during the penalty calculation
     */
    private List<String> updateActionWithStandardPermission(Long id, ActionRequest request, Action action, ArrayList<String> errorMessages) {
        actionValidatorService.validateRequest(
                request,
                action.getId(),
                Objects.equals(action.getActionTypeId(), request.getActionTypeId())
                        ? List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)
                        : List.of(NomenclatureItemStatus.ACTIVE),
                Objects.equals(action.getPenaltyClaimCurrencyId(), request.getPenaltyClaimAmountCurrencyId())
                        ? List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE)
                        : List.of(NomenclatureItemStatus.ACTIVE),
                errorMessages
        );

        ActionPenaltyCalculationResult actionPenaltyCalculationResult = penaltyCalculationService.calculatePenaltyAmount(request, action.getId());
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        actionPodService.processActionPods(request, action);
        ActionMapper.fromRequestToEntity(action, request, actionPenaltyCalculationResult);
        actionRepository.save(action);

        // TODO: 10/26/23 update communications (will be implemented in the following bundles)
        actionFileService.updateFiles(request.getFiles(), id, errorMessages);

        if ((Objects.nonNull(action.getPenaltyId()) || Objects.nonNull(action.getEmailTemplateId()))
                && isLiabilityGenerationApplicable(request.getPenaltyPayer(),
                request.getDontAllowAutomaticPenaltyClaim(), actionPenaltyCalculationResult, action.getClaimAmountManuallyEntered())) {
            customerLiabilityService.createLiabilityFromAction(action.getId());
        }
        return actionPenaltyCalculationResult.infoErrorMessages();
    }


    /**
     * Deletes an action if it is not executed and if liability is not already generated.
     *
     * @param id ID of the action to be deleted
     * @return ID of the deleted action
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting action with id: {}", id);

        Action action = actionRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Action not found by ID %s;".formatted(id)));

        if (action.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Action with ID {} is already deleted;", id);
            throw new OperationNotAllowedException("Action with ID %s is already deleted;".formatted(id));
        }

        if (action.getActionStatus().equals(ActionStatus.EXECUTED)) {
            log.error("You cannot delete an executed action;");
            throw new OperationNotAllowedException("You cannot delete an executed action;");
        }

        if (customerLiabilityService.isLiabilityGeneratedForAction(action.getId())) {
            log.error("You cannot delete an action in which liability is already generated;");
            throw new OperationNotAllowedException("You cannot delete an action in which liability is already generated;");
        }

        action.setStatus(EntityStatus.DELETED);
        actionRepository.save(action);
        return action.getId();
    }


    /**
     * Retrieves a list of actions by the given request.
     *
     * @param request {@link ActionListRequest} containing the criteria for the search
     * @return List of {@link ActionResponse}
     */
    public Page<ActionListResponse> list(ActionListRequest request) {
        log.debug("Listing actions by request: {}", request);

        ActionTableColumn sortBy = Objects.requireNonNullElse(request.getSortBy(), ActionTableColumn.DATE_OF_CREATION);
        ActionSearchField searchBy = Objects.requireNonNullElse(request.getSearchBy(), ActionSearchField.ALL);
        Sort.Direction sortDirection = Objects.requireNonNullElse(request.getSortDirection(), Sort.Direction.DESC);

        String actionPodsDirection = null;
        if (sortBy.equals(ActionTableColumn.POD_IDENTIFIERS)) {
            actionPodsDirection = sortDirection.name();
        }

        String penaltyClaimed = null;
        if (CollectionUtils.isNotEmpty(request.getPenaltyClaimed())) {
            if (request.getPenaltyClaimed().size() == 1) {
                // necessary to be string, for query syntax to work
                penaltyClaimed = String.valueOf(request.getPenaltyClaimed().get(0));
            }
        }

        return actionRepository
                .list(
                        searchBy.name(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        getStatusesByPermissions().stream().map(EntityStatus::name).toList(),
                        CollectionUtils.isEmpty(request.getActionStatuses()) ? new ArrayList<>() : request.getActionStatuses().stream().map(ActionStatus::name).toList(),
                        CollectionUtils.isEmpty(request.getActionTypeIds()) ? new ArrayList<>() : request.getActionTypeIds(),
                        request.getCreationDateFrom(),
                        request.getCreationDateTo(),
                        request.getNoticeReceivingDateFrom(),
                        request.getNoticeReceivingDateTo(),
                        request.getExecutionDateFrom(),
                        request.getExecutionDateTo(),
                        penaltyClaimed,
                        request.getCalculatedPenaltyFrom(),
                        request.getCalculatedPenaltyTo(),
                        CollectionUtils.isEmpty(request.getCurrencyIds()) ? new ArrayList<>() : request.getCurrencyIds(),
                        request.getClaimedAmountFrom(),
                        request.getClaimedAmountTo(),
                        CollectionUtils.isEmpty(request.getPenaltyPayers()) ? new ArrayList<>() : request.getPenaltyPayers().stream().map(ActionPenaltyPayer::name).toList(),
                        actionPodsDirection,
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                JpaSort.unsafe(sortDirection, sortBy.getValue())
                        )
                );
    }


    /**
     * @return List of statuses that the user is allowed to view
     */
    List<EntityStatus> getStatusesByPermissions() {
        List<EntityStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.ACTIONS, List.of(PermissionEnum.ACTIONS_VIEW_ACTIVE))) {
            statuses.add(EntityStatus.ACTIVE);
        }
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.ACTIONS, List.of(PermissionEnum.ACTIONS_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED);
        }
        return statuses;
    }


    @Transactional
    public void claimPenalty(Long id) {
        log.debug("Claiming penalty for action with id: {}", id);

        if (!permissionService.getPermissionsFromContext(PermissionContextEnum.ACTIONS).contains(PermissionEnum.ACTIONS_CLAIM_PENALTY.getId())) {
            log.error("You don't have permission claim penalty for action;");
            throw new ClientException("You don't have permission to claim penalty for action;", ErrorCode.OPERATION_NOT_ALLOWED);

        }
        Action action = actionRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Action not found by ID %s;".formatted(id)));

        if (action.getPenaltyPayer().equals(EPRES)) {
            log.error("You cannot claim penalty for action with id {} because EPRES is the penalty payer;", id);
            throw new OperationNotAllowedException("You cannot claim penalty for action with id %s because EPRES is the penalty payer;".formatted(id));
        }

        if (action.getPenaltyClaimAmount() == null) {
            log.error("You cannot claim penalty for action with id {} because penalty claim amount is not set;", id);
            throw new OperationNotAllowedException("You cannot claim penalty for action with id %s because penalty claim amount is not set;".formatted(id));
        }

        if (customerLiabilityService.isLiabilityGeneratedForAction(action.getId())) {
            log.error("You cannot claim penalty for action with id {} because liability is already generated;", id);
            throw new OperationNotAllowedException("You cannot claim penalty for action with id %s because liability is already generated;".formatted(id));
        }
        actionValidatorService.validatePenalty(action);
        customerLiabilityService.createLiabilityFromAction(action.getId());
        if (customerLiabilityService.isLiabilityGeneratedForAction(action.getId())) {
            actionDocumentCreationService.generateActionPenaltyDocumentAndSendEmail(action.getId());
        }
    }


    /**
     * Retrieves a customer by the given prompt, searches in the identifier field (exact match).
     * Intended for the customer sub-object dropdown in the action form.
     *
     * @param prompt Prompt to search by
     * @return {@link ActionCustomerResponse} containing the data of the retrieved customer
     */
    public ActionCustomerResponse getCustomer(String prompt) {
        log.debug("Getting customer for action by prompt: {}", prompt);
        return customerRepository.findCustomerForAction(StringUtil.underscoreReplacer(prompt));
    }


    /**
     * Retrieves a list of contracts by the given prompt and customer ID. Prompts are searched in the contract number field ("like" match)
     * and statuses: entered into force, active in term, active in perpetuity and terminated.
     * Intended for the contracts sub-object dropdown in the action form.
     *
     * @param prompt     Prompt to search by
     * @param customerId ID of the customer to search contracts for
     * @return product/service contracts any version of which matches with any version of customer
     */
    public Page<ActionContractResponse> filterContracts(String prompt, Long customerId, int page, int size) {
        log.debug("Filtering contracts for action by prompt: {} and customerId: {}", prompt, customerId);
        return productContractRepository.filterContractsForAction(
                EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(prompt)),
                customerId,
                PageRequest.of(page, size)
        );
    }


    /**
     * Retrieves a list of pods by the given identifier prompts and contract ID. Prompts are searched in the identifier field (exact match).
     * Intended for the pods sub-object dropdown in the action form.
     *
     * @param identifierPrompts List of prompts to search by
     * @param contractId        ID of the contract to any version of which the pods should be related
     * @return List of {@link ActionPodResponse} containing the data of the retrieved pods
     */
    public List<ActionPodResponse> searchPods(List<String> identifierPrompts, Long contractId) {
        log.debug("Searching pods for action by identifierPrompts: {} and contractId: {}", identifierPrompts, contractId);

        List<ActionPodResponse> persistedPods = podRepository.searchPodsForAction(identifierPrompts, contractId);
        if (persistedPods.isEmpty()) {
            log.error("No pods found for action by identifierPrompts: {} and contractId: {}", identifierPrompts, contractId);
            throw new DomainEntityNotFoundException("Pods (%s) not found for the selected contract.".formatted(String.join(", ", identifierPrompts)));
        }

        List<String> notFoundPods = new ArrayList<>();
        List<String> persistedPodIdentifiers = persistedPods
                .stream()
                .map(ActionPodResponse::identifier)
                .toList();
        for (String identifier : identifierPrompts) {
            if (!persistedPodIdentifiers.contains(identifier)) {
                notFoundPods.add(identifier);
            }
        }

        if (!notFoundPods.isEmpty()) {
            log.error("No pods found for action by identifierPrompts: {} and contractId: {}", notFoundPods, contractId);
            throw new DomainEntityNotFoundException("Pods (%s) not found for the selected contract.".formatted(String.join(", ", notFoundPods)));
        }

        return persistedPods;
    }


    /**
     * Retrieves a list of penalties by the given criteria. Prompts are searched in the name field ("like" match).
     * The penalty payer in penalty and action should match. Contract version and penalty group version is filtered
     * by the execution date of the action (it should fall between the version's range).
     * Intended for the penalties dropdown in the action form.
     *
     * @param request {@link ActionPenaltyRequest} containing the criteria for the search
     * @return List of {@link ActionPenaltyResponse} containing the data of the retrieved penalties
     */
    public Page<ActionPenaltyResponse> getAvailablePenalties(ActionPenaltyRequest request) {
        log.debug("Getting available penalties for action by request: {}", request);

        switch (request.getContractType()) {
            case PRODUCT_CONTRACT -> {
                return penaltyRepository.getAvailableProductPenaltiesForAction(
                        request.getContractId(),
                        request.getExecutionDate(),
                        request.getPenaltyPayer().name(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        null,
                        request.getActionTypeId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
            }
            case SERVICE_CONTRACT -> {
                return penaltyRepository.getAvailableServicePenaltiesForAction(
                        request.getContractId(),
                        request.getExecutionDate(),
                        request.getPenaltyPayer().name(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        null,
                        request.getActionTypeId(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
            }
            default -> {
                log.error("Contract type {} is not supported;", request.getContractType());
                throw new OperationNotAllowedException("Contract type %s is not supported;".formatted(request.getContractType()));
            }
        }
    }


    /**
     * Retrieves a list of terminations by the given criteria. Prompts are searched in the name field ("like" match).
     * The contract version is filtered by the execution date of the action (it should fall between the version's range).
     * Intended for the termination dropdown in the action form.
     *
     * @param request {@link ActionTerminationRequest} containing the criteria for the search
     * @return List of {@link ActionTerminationResponse} containing the data of the retrieved terminations
     */
    public Page<ActionTerminationResponse> getAvailableTerminations(ActionTerminationRequest request) {
        log.debug("Getting available terminations for action by request: {}", request);
        switch (request.getContractType()) {
            case PRODUCT_CONTRACT -> {
                return terminationRepository.getAvailableProductTerminationsForAction(
                        request.getContractId(),
                        request.getExecutionDate(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        null,
                        PageRequest.of(request.getPage(), request.getSize())
                );
            }
            case SERVICE_CONTRACT -> {
                return terminationRepository.getAvailableServiceTerminationsForAction(
                        request.getContractId(),
                        request.getExecutionDate(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        null,
                        PageRequest.of(request.getPage(), request.getSize())
                );
            }
            default -> {
                log.error("Contract type {} is not supported;", request.getContractType());
                throw new OperationNotAllowedException("Contract type %s is not supported;".formatted(request.getContractType()));
            }
        }
    }


    /**
     * Retrieves a list of actions, which are active, with status 'AWAITING' and their execution date is today or before today.
     */
    @ExecutionTimeLogger
    public void transitionStatusOfEligibleActionsToExecuted() {
        log.debug("Transitioning status of eligible actions to executed");

        List<Action> actions = actionRepository.findEligibleActionsForStatusTransitionByExecutionDate();

        actions.forEach(action -> {
            try {
                action.setActionStatus(ActionStatus.EXECUTED);
                actionRepository.save(action);
            } catch (Exception e) {
                log.error("Error while transitioning status of action with id: {} to executed.", action.getId(), e);
            }
        });
    }

    public ActionPenaltyCalculationResult calculatePenaltyAmount(CalculatePenaltyAmountRequest request) {
        log.debug("Calculating penalty amount with request: {}", request);
        List<String> errorMessages = new ArrayList<>();
        ActionRequest actionRequest = new ActionRequest();

        actionRequest.setActionTypeId(request.getActionTypeId());
        actionRequest.setExecutionDate(request.getExecutionDate());
        actionRequest.setPenaltyId(request.getPenaltyId());
        actionRequest.setTerminationId(request.getTerminationId());
        actionRequest.setContractId(request.getContractId());
        actionRequest.setContractType(request.getContractType());
        actionRequest.setPods(request.getPods());
        actionRequest.setPenaltyPayer(request.getPenaltyPayer());
        actionRequest.setCustomerId(request.getCustomerId());

        actionValidatorService.validateCalculatePenaltyAmountRequest(actionRequest, List.of(NomenclatureItemStatus.ACTIVE), errorMessages);

        return penaltyCalculationService.calculatePenaltyAmount(actionRequest, null);
    }

    private boolean isLiabilityGenerationApplicable(ActionPenaltyPayer penaltyPayer,
                                                    Boolean dontAllowAutomaticPenaltyClaim,
                                                    ActionPenaltyCalculationResult actionPenaltyCalculationResult,
                                                    Boolean cLaimAmountManuallyEntered) {
        if (!penaltyPayer.equals(EPRES) && BooleanUtils.isFalse(dontAllowAutomaticPenaltyClaim)) {
            return (actionPenaltyCalculationResult.isNotEmpty() &&
                    BooleanUtils.isTrue(actionPenaltyCalculationResult.isAutomaticClaimSelectedInPenalty())) || cLaimAmountManuallyEntered;
        }

        return false;
    }


    private void validateAndSetTemplate(Long templateId, Action action, ContractTemplateType type, List<String> messages) {
        if (action != null && ((type == ContractTemplateType.EMAIL && Objects.equals(action.getEmailTemplateId(), templateId))
                || (type == ContractTemplateType.DOCUMENT && Objects.equals(action.getTemplateId(), templateId)))
        ) {
            return;
        }
        if (templateId == null) {
            return;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.PENALTY, type, LocalDate.now())) {
            messages.add("emailTemplateId-Template with id %s do not exist!;".formatted(templateId));
        }

    }
}
