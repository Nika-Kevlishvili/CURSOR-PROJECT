package bg.energo.phoenix.service.receivable.massOperationForBlocking;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlocking;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlockingExclusionPrefix;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlockingTask;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.BlockingSelection;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.request.receivable.massOperationForBlocking.*;
import bg.energo.phoenix.model.response.receivable.ValidateConditionResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ConditionEvaluationResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ReceivableBlockingListingResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ReceivableBlockingResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.BlockingReasonRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingExclusionPrefixesRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingTaskRepository;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableBlockingService {

    private final ReceivableBlockingExclusionPrefixesRepository receivableBlockingExclusionPrefixesRepository;
    private final ReceivableBlockingPermissionService receivableBlockingPermissionService;
    private final ReceivableBlockingConditionService receivableBlockingConditionService;
    private final ReceivableBlockingRepository receivableBlockingRepository;
    private final ReceivableBlockingMapper receivableBlockingMapper;
    private final BlockingReasonRepository blockingReasonRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final PrefixRepository prefixRepository;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final ReceivableBlockingTaskRepository receivableBlockingTasksRepository;

    @Transactional
    public Long create(ReceivableBlockingCreateRequest request) {
        log.info("Create receivable mass operation for blocking: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();

        if (StringUtils.isNotBlank(request.getConditions())) {
            receivableBlockingConditionService.validateCondition(request.getConditions(), errorMessages);
            receivableBlockingConditionService.validateConditionKeys(request.getConditions(), errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        }

        if (receivableBlockingRepository.existsByName(request.getName())) {
            errorMessages.add("name-receivable blocking with the name %s already exists".formatted(request.getName()));
        }

        ReceivableBlockingStatus status = receivableBlockingPermissionService.checkAndGetBlockingStatusByPermissionOnCreate(request.getRequestReceivableBlockingStatus());
        ReceivableBlocking receivableBlocking = receivableBlockingMapper.fromCreateRequestToEntity(request);
        receivableBlocking.setBlockingStatus(status);
        receivableBlocking.setListOfCustomers(splitCheckAndSetCustomers(request.getListOfCustomers(), errorMessages));

        checkGetAndSetCurrencyOnCreate(request.getExclusionByAmount(), receivableBlocking, errorMessages);

        checkGetAndSetBlockingReasonForPayment(request.getBlockingForPayment(), receivableBlocking, errorMessages);
        checkGetAndSetBlockingReasonForCalculation(request.getBlockingForCalculation(), receivableBlocking, errorMessages);
        checkGetAndSetBlockingReasonForReminderLetters(request.getBlockingForReminderLetters(), receivableBlocking, errorMessages);
        checkGetAndSetBlockingReasonForSupplyTermination(request.getBlockingForSupplyTermination(), receivableBlocking, errorMessages);
        checkGetAndSetBlockingReasonForLiabilitiesOffsetting(request.getBlockingForLiabilitiesOffsetting(), receivableBlocking, errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        receivableBlockingRepository.save(receivableBlocking);

        checkGetAndSavePrefixesOnCreate(request.getPrefixNomenclatureIds(), receivableBlocking.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return receivableBlocking.getId();
    }

    @Transactional
    public Long update(Long id, ReceivableBlockingEditRequest request) {
        log.info("updating receivable blocking with id: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();

        Optional<ReceivableBlocking> receivableBlockingOptional = receivableBlockingRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
        if (receivableBlockingOptional.isEmpty()) {
            throw new ClientException("Can't find active receivable blocking with id: %s;".formatted(id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND);
        }

        ReceivableBlocking receivableBlockingEntity = receivableBlockingOptional.get();
        if(receivableBlockingEntity.getBlockingStatus().equals(ReceivableBlockingStatus.EXECUTED) && request.getRequestReceivableBlockingStatus().equals(ReceivableBlockingStatus.DRAFT)) {
            throw new ClientException("Cant change blocking status from executed to draft!",ErrorCode.CONFLICT);
        }
        String customerConditions = receivableBlockingEntity.getCustomerConditions();
        if(StringUtils.isNotBlank(customerConditions)){
            if (!customerConditions.equals(request.getConditions())) {
                if(!receivableBlockingEntity.getBlockingStatus().equals(ReceivableBlockingStatus.EXECUTED)) {
                    receivableBlockingConditionService.validateCondition(request.getConditions(), errorMessages);
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                } else {
                    errorMessages.add("Can't change condition while receivable blocking is executed: %s;".formatted(request.getConditions()));
                }
            }
        } else {
            String requestCondition = request.getConditions();
            if(StringUtils.isNotBlank(requestCondition)){
                receivableBlockingConditionService.validateCondition(requestCondition, errorMessages);
                EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            }
        }

        ReceivableBlockingStatus status = receivableBlockingPermissionService.checkAndGetBlockingStatusByPermissionOnEdit(request.getRequestReceivableBlockingStatus());
        receivableBlockingEntity.setBlockingStatus(status);
        receivableBlockingEntity.setListOfCustomers(splitCheckAndSetCustomers(request.getListOfCustomers(), errorMessages));

        checkGetAndSetPrefixesOnEdit(request.getPrefixNomenclatureIds(), receivableBlockingEntity.getId(), errorMessages);
        checkGetAndSetCurrencyOnEdit(request.getExclusionByAmount(), receivableBlockingEntity, errorMessages);

        receivableBlockingMapper.fromEditRequestToEntity(request, receivableBlockingEntity);
        editBlockingReasonForPayment(request.getBlockingForPayment(), receivableBlockingEntity, errorMessages);
        editBlockingReasonForCalculation(request.getBlockingForCalculation(), receivableBlockingEntity, errorMessages);
        editBlockingReasonForReminderLetters(request.getBlockingForReminderLetters(), receivableBlockingEntity, errorMessages);
        editBlockingReasonForSupplyTermination(request.getBlockingForSupplyTermination(), receivableBlockingEntity, errorMessages);
        editBlockingReasonForLiabilitiesOffsetting(request.getBlockingForLiabilitiesOffsetting(), receivableBlockingEntity, errorMessages);
        addEditTaskToReceivableBlocking(receivableBlockingEntity, request.getTaskIds(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        receivableBlockingRepository.save(receivableBlockingEntity);
        return receivableBlockingEntity.getId();
    }

    @Transactional
    public Long delete(Long id) {
        log.info("Deleting receivable blocking with id: {}", id);

        ReceivableBlocking receivableBlocking = receivableBlockingRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Receivable blocking not found by ID %s;".formatted(id)));

        if (receivableBlocking.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Receivable blocking with id: {} is already deleted", id);
            throw new OperationNotAllowedException("Receivable blocking is already deleted;");
        }

        receivableBlockingPermissionService.checkOnDeletePermissionByBlockingStatus(receivableBlocking.getBlockingStatus());

        receivableBlocking.setStatus(EntityStatus.DELETED);
        receivableBlockingRepository.save(receivableBlocking);
        return id;
    }

    public ReceivableBlockingResponse view(Long id) {
        log.info("view for receivable blocking with id: %s ".formatted(id));

        ReceivableBlocking receivableBlocking = receivableBlockingRepository
                .findById(id)
                .orElseThrow(() -> new ClientException("Can't find receivable blocking with id: %s;".formatted(id), ErrorCode.CONFLICT));

        receivableBlockingPermissionService.checkOnViewPermissionsByStatuses(receivableBlocking.getStatus(), receivableBlocking.getBlockingStatus());

        ReceivableBlockingResponse response = receivableBlockingMapper.fromEntityToPreviewResponse(receivableBlocking);
        fetchAndMapConditionsAndConditionsInfo(receivableBlocking.getCustomerConditions(), response);
        response.setTaskShortResponse(getTasks(id));
        return response;
    }

    //Create
    private void checkGetAndSetCurrencyOnCreate(ExclusionByAmountRequest exclusionByAmount, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (exclusionByAmount != null) {
            Long currencyId = exclusionByAmount.getCurrency();
            Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
            if (currencyOptional.isPresent()) {
                receivableBlocking.setCurrencyId(currencyOptional.get().getId());
            } else {
                errorMessages.add("exclusionByAmount.currency-[exclusionByAmount.currency] active currency with: %s can't be found;".formatted(currencyId));
            }
        }
    }

    private void checkGetAndSetBlockingReasonForPayment(BlockingForPaymentRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (request != null) {
            Long reasonId = checkAndGetReasonIdOnCreate(request.getReasonId(), request.getReasonType());
            if (reasonId != null) {
                mapForAddingBlockingReason(
                        receivableBlocking,
                        request.getReasonType(),
                        request.getToDate(),
                        request.getFromDate(),
                        request.getAdditionalInformation(),
                        reasonId
                );
            } else {
                errorMessages.add(getErrorMessageForReasonId("blockingForPayment", request.getReasonId()));
            }
        }
    }

    private void checkGetAndSetBlockingReasonForCalculation(BlockingForCalculationRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (request != null) {
            Long reasonId = checkAndGetReasonIdOnCreate(request.getReasonId(), request.getReasonType());
            if (reasonId != null) {
                mapForAddingBlockingReason(
                        receivableBlocking,
                        request.getReasonType(),
                        request.getToDate(),
                        request.getFromDate(),
                        request.getAdditionalInformation(),
                        reasonId
                );
            } else {
                errorMessages.add(getErrorMessageForReasonId("blockingForCalculation", request.getReasonId()));
            }
        }
    }

    private void checkGetAndSetBlockingReasonForReminderLetters(BlockingForReminderLettersRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (request != null) {
            Long reasonId = checkAndGetReasonIdOnCreate(request.getReasonId(), request.getReasonType());
            if (reasonId != null) {
                mapForAddingBlockingReason(
                        receivableBlocking,
                        request.getReasonType(),
                        request.getToDate(),
                        request.getFromDate(),
                        request.getAdditionalInformation(),
                        reasonId
                );
            } else {
                errorMessages.add(getErrorMessageForReasonId("blockingForReminderLetters", request.getReasonId()));
            }
        }
    }

    private void checkGetAndSetBlockingReasonForSupplyTermination(BlockingForSupplyTerminationRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (request != null) {
            Long reasonId = checkAndGetReasonIdOnCreate(request.getReasonId(), request.getReasonType());
            if (reasonId != null) {
                mapForAddingBlockingReason(
                        receivableBlocking,
                        request.getReasonType(),
                        request.getToDate(),
                        request.getFromDate(),
                        request.getAdditionalInformation(),
                        reasonId
                );
            } else {
                errorMessages.add(getErrorMessageForReasonId("blockingForSupplyTermination", request.getReasonId()));
            }
        }
    }

    private void checkGetAndSetBlockingReasonForLiabilitiesOffsetting(BlockingForLiabilitiesOffsettingRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (request != null) {
            Long reasonId = checkAndGetReasonIdOnCreate(request.getReasonId(), request.getReasonType());
            if (reasonId != null) {
                mapForAddingBlockingReason(
                        receivableBlocking,
                        request.getReasonType(),
                        request.getToDate(),
                        request.getFromDate(),
                        request.getAdditionalInformation(),
                        reasonId
                );
            } else {
                errorMessages.add(getErrorMessageForReasonId("blockingForLiabilitiesOffsetting", request.getReasonId()));
            }
        }
    }

    private void checkGetAndSavePrefixesOnCreate(List<Long> prefixes, Long receivableBlockingId, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(prefixes)) {
            for (int i = 0; i < prefixes.size(); i++) {
                Long currentPrefixId = prefixes.get(i);
                Optional<Prefix> prefixOptional = prefixRepository.findByIdAndStatusIn(currentPrefixId, List.of(NomenclatureItemStatus.ACTIVE));
                if (prefixOptional.isPresent()) {
                    ReceivableBlockingExclusionPrefix receivableBlockingExclusionPrefix = receivableBlockingMapper.toRelateExclusionPrefix(currentPrefixId, receivableBlockingId);
                    receivableBlockingExclusionPrefixesRepository.save(receivableBlockingExclusionPrefix);
                } else {
                    errorMessages.add("prefixNomenclatureIds[%s]-Can't find active prefix with id:%s;".formatted(i, currentPrefixId));
                }
            }
        }
    }

    private Long checkAndGetReasonIdOnCreate(Long targetReasonId, ReceivableBlockingReasonType reasonType) {
        Optional<BlockingReason> blockingReasonOptional = blockingReasonRepository.findByIdAndStatusAndReasonType(
                targetReasonId,
                List.of(NomenclatureItemStatus.ACTIVE),
                EPBStringUtils.fromPromptToQueryParameter(reasonType.name())
        );
        return blockingReasonOptional.map(BlockingReason::getId).orElse(null);
    }

    private String splitCheckAndSetCustomers(String customers, List<String> errorMessages) {
        if (StringUtils.isNotBlank(customers)) {
            Set<String> listOfNames = Arrays.stream(customers.split(",")).map(String::trim).collect(Collectors.toSet());
            checkCustomer(listOfNames, errorMessages);
        }
        return StringUtils.isNotBlank(customers) ? customers.replaceAll("\\s","") : customers;
    }

    private void checkCustomer(Set<String> listOfNames, List<String> errorMessages) {
        for (String item : listOfNames) {
            Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(item.trim(), CustomerStatus.ACTIVE);
            if (customer.isEmpty()) {
                errorMessages.add("listOfCustomers-[listOfCustomers] customer with: %s can't be found;".formatted(item.trim()));
            }
        }
    }

    //Condition
    public ValidateConditionResponse validateCondition(ReceivableBlockingConditionValidationRequest request) {
        List<String> errorMessages = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCondition())) {
            receivableBlockingConditionService.validateCondition(request.getCondition(), errorMessages);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return new ValidateConditionResponse(true);
    }

    /**
     * Evaluates the condition for the specified receivable blocking and returns the result.
     *
     * @param blockingId the ID of the receivable blocking to evaluate the condition for
     * @return the result of evaluating the condition for the specified receivable blocking
     */
    public ConditionEvaluationResponse conditionResult(Long blockingId) {
        return receivableBlockingConditionService.conditionResult(blockingId);
    }

    private void fetchAndMapConditionsAndConditionsInfo(String condition, ReceivableBlockingResponse response) {
        response.setConditionsInfo(receivableBlockingConditionService.getConditionsInfo(condition));
        response.setConditions(condition);
    }

    //Listing
    public Page<ReceivableBlockingListingResponse> list(ReceivableBlockingListingRequest request) {
        Boolean blockedForPayment = determineBySelection(request.getBlockedForPayment());
        Boolean blockedForLetters = determineBySelection(request.getBlockedForLetters());
        Boolean blockedForCalculations = determineBySelection(request.getBlockedForCalculations());
        Boolean blockedForLiabilities = determineBySelection(request.getBlockedForLiabilities());
        Boolean blockedForTermination = determineBySelection(request.getBlockedForTermination());

        return receivableBlockingRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        getSearchByEnum(request.getSearchField()),
                        blockedForPayment,
                        blockedForLetters,
                        blockedForCalculations,
                        blockedForLiabilities,
                        blockedForTermination,
                        receivableBlockingPermissionService.filterBlockingStatusByPermissionOnListing(),
                        receivableBlockingPermissionService.filterBlockingDeletedStatusByPermissionOnListing(),
                        CollectionUtils.isEmpty(request.getStatuses()) ? List.of(EntityStatus.ACTIVE.name(),EntityStatus.DELETED.name())
                                 : request.getStatuses().stream().map(Enum::name).toList(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSorByEnum(request.getColumn()))
                                )
                        )
                )
                .map(ReceivableBlockingListingResponse::new);
    }

    private Boolean determineBySelection(BlockingSelection blockingSelection) {
        if(blockingSelection==null) return null;
        Boolean result = null;
        if(blockingSelection.equals(BlockingSelection.NO)) {
            result=false;
        } else if(blockingSelection.equals(BlockingSelection.YES)) {
            result = true;
        }
        return result;
    }

    private String getSearchByEnum(ReceivableBlockingListingSearchField searchField) {
        return searchField != null ? searchField.getValue() : ReceivableBlockingListingSearchField.ALL.getValue();
    }

    private String getSorByEnum(ReceivableBlockingListingColumn sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : ReceivableBlockingListingColumn.ID.getValue();
    }

    //Edit
    private void checkGetAndSetCurrencyOnEdit(ExclusionByAmountRequest exclusionByAmount, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (exclusionByAmount != null) {
            Long currencyId = exclusionByAmount.getCurrency();
            if (!Objects.equals(receivableBlocking.getCurrencyId(), currencyId)) {
                Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
                if (currencyOptional.isPresent()) {
                    receivableBlocking.setCurrencyId(currencyOptional.get().getId());
                } else {
                    errorMessages.add("exclusionByAmount.currency-[exclusionByAmount.currency] active currency with: %s can't be found;".formatted(currencyId));
                }
            }
        } else {
            receivableBlocking.setCurrencyId(null);
        }
    }

    private void checkGetAndSetPrefixesOnEdit(List<Long> requestedPrefixIds, Long receivableBlockingId, List<String> errorMessages) {
        List<ReceivableBlockingExclusionPrefix> exclusionPrefixesFromDb = getRelatedPrefixes(receivableBlockingId);
        List<Long> oldPrefixes = exclusionPrefixesFromDb.stream().map(ReceivableBlockingExclusionPrefix::getPrefixId).toList();
        List<Long> prefixIds = requestedPrefixIds == null ? new ArrayList<>() : requestedPrefixIds;

        List<Long> added = EPBListUtils.getAddedElementsFromList(oldPrefixes, prefixIds);
        checkGetAndSavePrefixesOnCreate(added, receivableBlockingId, errorMessages);

        List<Long> deleted = EPBListUtils.getDeletedElementsFromList(oldPrefixes, prefixIds);
        deleteRelatedPrefixes(deleted, exclusionPrefixesFromDb);
    }

    private Long checkAndGetReasonIdOnEdit(Long currentReasonId, Long targetReasonId, ReceivableBlockingReasonType reasonType) {
        if (!Objects.equals(currentReasonId, targetReasonId)) {
            Optional<BlockingReason> blockingReasonOptional = blockingReasonRepository.findByIdAndStatusAndReasonType(
                    targetReasonId,
                    List.of(NomenclatureItemStatus.ACTIVE),
                    EPBStringUtils.fromPromptToQueryParameter(reasonType.name())
            );
            return blockingReasonOptional.map(BlockingReason::getId).orElse(null);
        }
        return currentReasonId;
    }

    private void deleteRelatedPrefixes(List<Long> deletedIds, List<ReceivableBlockingExclusionPrefix> exclusionPrefixesFromDb) {
        deletedIds
                .stream()
                .filter(Objects::nonNull)
                .map(aLong -> exclusionPrefixesFromDb
                        .stream()
                        .filter(exclusionPrefix -> Objects.equals(exclusionPrefix.getPrefixId(), aLong))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(exclusionPrefix -> {
                            exclusionPrefix.setStatus(ReceivableSubObjectStatus.DELETED);
                            receivableBlockingExclusionPrefixesRepository.save(exclusionPrefix);
                        }
                );
    }

    private List<ReceivableBlockingExclusionPrefix> getRelatedPrefixes(Long receivableBlockingId) {
        Optional<List<ReceivableBlockingExclusionPrefix>> exclusionPrefixesOptional = receivableBlockingExclusionPrefixesRepository.findByReceivableBlockingIdAndStatus(receivableBlockingId, ReceivableSubObjectStatus.ACTIVE);
        return exclusionPrefixesOptional.orElseGet(ArrayList::new);
    }

    private void editBlockingReasonForPayment(BlockingForPaymentRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (blockingTypeAlreadyExists(receivableBlocking.getBlockedForPayment())) {
            if (request == null) {
                receivableBlockingMapper.removeBlockingReasonByType(receivableBlocking, ReceivableBlockingReasonType.BLOCKED_FOR_PAYMENT);
            } else {
                Long reasonId = checkAndGetReasonIdOnEdit(receivableBlocking.getBlockedForPaymentReasonId(), request.getReasonId(), request.getReasonType());
                if (reasonId == null) {
                    errorMessages.add(getErrorMessageForReasonId("blockingForPayment", request.getReasonId()));
                } else {
                    mapForAddingBlockingReason(
                            receivableBlocking,
                            request.getReasonType(),
                            request.getToDate(),
                            request.getFromDate(),
                            request.getAdditionalInformation(),
                            reasonId
                    );
                }
            }
        } else {
            checkGetAndSetBlockingReasonForPayment(request, receivableBlocking, errorMessages);
        }
    }

    private void editBlockingReasonForCalculation(BlockingForCalculationRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (blockingTypeAlreadyExists(receivableBlocking.getBlockedForPayment())) {
            if (request == null) {
                receivableBlockingMapper.removeBlockingReasonByType(receivableBlocking, ReceivableBlockingReasonType.BLOCKED_FOR_CALC_LATE_PAYMENT_FINES_INTERESTS);
            } else {
                Long reasonId = checkAndGetReasonIdOnEdit(receivableBlocking.getBlockedForPaymentReasonId(), request.getReasonId(), request.getReasonType());
                if (reasonId == null) {
                    errorMessages.add(getErrorMessageForReasonId("blockingForCalculation", request.getReasonId()));
                } else {
                    mapForAddingBlockingReason(
                            receivableBlocking,
                            request.getReasonType(),
                            request.getToDate(),
                            request.getFromDate(),
                            request.getAdditionalInformation(),
                            reasonId
                    );
                }
            }
        } else {
            checkGetAndSetBlockingReasonForCalculation(request, receivableBlocking, errorMessages);
        }
    }

    private void editBlockingReasonForReminderLetters(BlockingForReminderLettersRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (blockingTypeAlreadyExists(receivableBlocking.getBlockedForPayment())) {
            if (request == null) {
                receivableBlockingMapper.removeBlockingReasonByType(receivableBlocking, ReceivableBlockingReasonType.BLOCKED_FOR_REMINDER_LETTERS);
            } else {
                Long reasonId = checkAndGetReasonIdOnEdit(receivableBlocking.getBlockedForPaymentReasonId(), request.getReasonId(), request.getReasonType());
                if (reasonId == null) {
                    errorMessages.add(getErrorMessageForReasonId("blockingForReminderLetters", request.getReasonId()));
                } else {
                    mapForAddingBlockingReason(
                            receivableBlocking,
                            request.getReasonType(),
                            request.getToDate(),
                            request.getFromDate(),
                            request.getAdditionalInformation(),
                            reasonId
                    );
                }
            }
        } else {
            checkGetAndSetBlockingReasonForReminderLetters(request, receivableBlocking, errorMessages);
        }
    }

    private void editBlockingReasonForSupplyTermination(BlockingForSupplyTerminationRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (blockingTypeAlreadyExists(receivableBlocking.getBlockedForPayment())) {
            if (request == null) {
                receivableBlockingMapper.removeBlockingReasonByType(receivableBlocking, ReceivableBlockingReasonType.BLOCKED_FOR_SUPPLY_TERMINATION);
            } else {
                Long reasonId = checkAndGetReasonIdOnEdit(receivableBlocking.getBlockedForPaymentReasonId(), request.getReasonId(), request.getReasonType());
                if (reasonId == null) {
                    errorMessages.add(getErrorMessageForReasonId("blockingForSupplyTermination", request.getReasonId()));
                } else {
                    mapForAddingBlockingReason(
                            receivableBlocking,
                            request.getReasonType(),
                            request.getToDate(),
                            request.getFromDate(),
                            request.getAdditionalInformation(),
                            reasonId
                    );
                }
            }
        } else {
            checkGetAndSetBlockingReasonForSupplyTermination(request, receivableBlocking, errorMessages);
        }
    }

    private void editBlockingReasonForLiabilitiesOffsetting(BlockingForLiabilitiesOffsettingRequest request, ReceivableBlocking receivableBlocking, List<String> errorMessages) {
        if (blockingTypeAlreadyExists(receivableBlocking.getBlockedForPayment())) {
            if (request == null) {
                receivableBlockingMapper.removeBlockingReasonByType(receivableBlocking, ReceivableBlockingReasonType.BLOCKED_FOR_LIABILITIES_OFFSETTING);
            } else {
                Long reasonId = checkAndGetReasonIdOnEdit(receivableBlocking.getBlockedForPaymentReasonId(), request.getReasonId(), request.getReasonType());
                if (reasonId == null) {
                    errorMessages.add(getErrorMessageForReasonId("blockingForLiabilitiesOffsetting", request.getReasonId()));
                } else {
                    mapForAddingBlockingReason(
                            receivableBlocking,
                            request.getReasonType(),
                            request.getToDate(),
                            request.getFromDate(),
                            request.getAdditionalInformation(),
                            reasonId
                    );
                }
            }
        } else {
            checkGetAndSetBlockingReasonForLiabilitiesOffsetting(request, receivableBlocking, errorMessages);
        }
    }

    private void mapForAddingBlockingReason(ReceivableBlocking receivableBlocking,
                                            ReceivableBlockingReasonType reasonType,
                                            LocalDate toDate,
                                            LocalDate fromDate,
                                            String information,
                                            Long reasonId
    ) {
        receivableBlockingMapper.fromBlockingReasonRequestToEntity(
                receivableBlocking,
                reasonType,
                toDate,
                fromDate,
                information,
                reasonId
        );
    }

    private void addEditTaskToReceivableBlocking(ReceivableBlocking updatedReceivableBlocking, List<Long> taskIds, List<String> errorMassages) {
        log.info("Add/edit receivable blocking task for receivable blocking with id: %s;".formatted(updatedReceivableBlocking.getId()));

        if (!CollectionUtils.isEmpty(taskIds)) {
            removeAllTasksOtherThanRequestTaskIds(taskIds);

            List<Task> tasks = taskRepository.findByIdInAndStatusInAndConnectionType(taskIds, List.of(EntityStatus.ACTIVE), TaskConnectionType.RECEIVABLES);
            Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, Function.identity()));
            List<ReceivableBlockingTask> receivableBlockingTasks = receivableBlockingTasksRepository.findByReceivableBlockingIdAndTaskIdInAndStatusIn(
                    updatedReceivableBlocking.getId(),
                    taskIds,
                    List.of(ReceivableSubObjectStatus.ACTIVE));
            Map<Long, ReceivableBlockingTask> receivableBlockingTasksMap = receivableBlockingTasks.stream()
                    .collect(Collectors.toMap(ReceivableBlockingTask::getTaskId, Function.identity()));
            List<ReceivableBlockingTask> newReceivableBlockingTasks = new ArrayList<>();

            for (int i = 0; i < taskIds.size(); i++) {
                Long id = taskIds.get(i);
                Task task = taskMap.get(id);
                if (task != null) {
                    ReceivableBlockingTask receivableBlockingTask = receivableBlockingTasksMap.get(id);
                    if (receivableBlockingTask != null) {
                        receivableBlockingTask.setModifyDate(LocalDateTime.now());
                    } else {
                        ReceivableBlockingTask newReceivableBlockingTask = createReceivableBlockingTask(updatedReceivableBlocking, task);
                        newReceivableBlockingTasks.add(newReceivableBlockingTask);
                    }
                } else {
                    errorMassages.add("taskId[%s]-Can't find active Task with id:%s;".formatted(i, taskIds));
                }

                receivableBlockingTasksRepository.saveAll(newReceivableBlockingTasks);
            }
        } else {
            List<ReceivableBlockingTask> receivableBlockingTasksList =
                    receivableBlockingTasksRepository.findByReceivableBlockingIdAndStatusIn(
                            updatedReceivableBlocking.getId(),
                            List.of(ReceivableSubObjectStatus.ACTIVE));
            List<ReceivableBlockingTask> receivableBlockingTasksToDelete = new ArrayList<>();
            if (!CollectionUtils.isEmpty(receivableBlockingTasksList)) {
                for (ReceivableBlockingTask item : receivableBlockingTasksList) {
                    item.setStatus(ReceivableSubObjectStatus.DELETED);
                    receivableBlockingTasksToDelete.add(item);
                }
                if (!CollectionUtils.isEmpty(receivableBlockingTasksToDelete)) {
                    receivableBlockingTasksRepository.saveAll(receivableBlockingTasksToDelete);
                }
            }
        }
    }

    private void removeAllTasksOtherThanRequestTaskIds(List<Long> taskIds) {
        List<ReceivableBlockingTask> receivableBlockingTasksList =
                receivableBlockingTasksRepository.findByTaskIdNotInAndStatusIn(
                        taskIds,
                        List.of(ReceivableSubObjectStatus.ACTIVE));
        List<ReceivableBlockingTask> receivableBlockingTasksListToDelete = new ArrayList<>();
        if (!CollectionUtils.isEmpty(receivableBlockingTasksList)) {
            for (ReceivableBlockingTask item : receivableBlockingTasksList) {
                item.setStatus(ReceivableSubObjectStatus.DELETED);
                receivableBlockingTasksListToDelete.add(item);
            }
            receivableBlockingTasksRepository.saveAll(receivableBlockingTasksListToDelete);
        }
    }

    private ReceivableBlockingTask createReceivableBlockingTask(ReceivableBlocking updatedReceivableBlocking, Task task) {
        ReceivableBlockingTask receivableBlockingTasks = new ReceivableBlockingTask();
        receivableBlockingTasks.setReceivableBlockingId(updatedReceivableBlocking.getId());
        receivableBlockingTasks.setTaskId(task.getId());
        receivableBlockingTasks.setStatus(ReceivableSubObjectStatus.ACTIVE);
        return receivableBlockingTasks;
    }

    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByReceivableBlockingId(id);
    }

    private boolean blockingTypeAlreadyExists(Boolean type) {
        return Objects.nonNull(type) && type;
    }

    private String getErrorMessageForReasonId(String requestPath, Long reasonId) {
        return "%s.reasonId-[reasonId] active reason with id: %s can't be found;".formatted(requestPath, reasonId);
    }

}
