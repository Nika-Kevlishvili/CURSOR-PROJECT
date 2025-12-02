package bg.energo.phoenix.service.product.penalty.penalty;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.contract.ActionType;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.product.penalty.penalty.*;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductPenalty;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServicePenalty;
import bg.energo.phoenix.model.enums.copy.domain.CopyDomain;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyAvailability;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyFormulaVariable;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.copy.domain.CopyDomainBaseRequest;
import bg.energo.phoenix.model.request.product.penalty.penalty.AvailablePenaltySearchRequest;
import bg.energo.phoenix.model.request.product.penalty.penalty.PenaltyListRequest;
import bg.energo.phoenix.model.request.product.penalty.penalty.PenaltyRequest;
import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.PenaltyPaymentTermRequest;
import bg.energo.phoenix.model.response.contract.action.calculation.formula.PenaltyFormulaVariablesExtractionResult;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.penalty.*;
import bg.energo.phoenix.model.response.penalty.copy.PenaltyPaymentTermsCopyResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.contract.ActionTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyActionTypesRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyPaymentTermRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductPenaltyRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServicePenaltyRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.domain.CopyDomainBaseService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.OPERATION_NOT_ALLOWED;
import static bg.energo.phoenix.model.entity.EntityStatus.DELETED;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PENALTY;
import static bg.energo.phoenix.permissions.PermissionEnum.PENALTY_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.PENALTY_VIEW_DELETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class PenaltyService implements CopyDomainBaseService {
    private final ContractTemplateRepository contractTemplateRepository;
    private final ServicePenaltyRepository servicePenaltyRepository;
    private final ProductPenaltyRepository productPenaltyRepository;
    private final PenaltyRepository penaltyRepository;
    private final PenaltyDataMapper penaltyDataMapper;
    private final CurrencyRepository currencyRepository;
    private final CalendarRepository calendarRepository;
    private final PenaltyPaymentTermRepository penaltyPaymentTermRepository;
    private final PermissionService permissionService;
    private final ActionTypeRepository actionTypeRepository;
    private final PriceParameterDetailsRepository priceParameterDetailsRepository;
    private final PenaltyActionTypesRepository penaltyActionTypesRepository;


    /**
     * Retrieves penalties by the search criteria provided in the request
     *
     * @param request the request containing the search criteria
     * @return the list of penalties matching the search criteria
     */
    public Page<PenaltyListResponse> list(PenaltyListRequest request) {
        log.debug("Fetching penalty list for the following request: {}", request);

        String sortBy = PenaltyTableColumn.ID.getValue();
        if (request.getSortBy() != null) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = PenaltySearchField.ALL.getValue();
        if (request.getSearchBy() != null) {
            searchBy = request.getSearchBy().getValue();
        }

        List<EntityStatus> entityStatuses = getStatusesByPermissions();

        String penaltyReceivingParties = null;
        if (request.getPenaltyReceivingParties() != null) {
            penaltyReceivingParties = "{";
            penaltyReceivingParties = penaltyReceivingParties + request.getPenaltyReceivingParties().stream().map(PartyReceivingPenalty::name)
                    .collect(Collectors.joining(","));
            penaltyReceivingParties = penaltyReceivingParties + "}";
        }

        Page<PenaltyListMiddleResponse> penalties = penaltyRepository
                .findAll(
                        searchBy,
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        penaltyReceivingParties,
                        request.getApplicability() == null ? new ArrayList<>() : request.getApplicability().stream().map(PenaltyApplicability::name).toList(),
                        entityStatuses.stream().map(EntityStatus::name).toList(),
                        request.getAvailable() == PenaltyAvailability.ALL ? null : request.getAvailable().toString(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(new Sort.Order(sortDirection, sortBy))
                        )
                );

        return penalties
                .map(PenaltyListResponse::new);
    }

    private List<EntityStatus> getStatusesByPermissions() {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        if (permissionService.getPermissionsFromContext(PENALTY).contains(PENALTY_VIEW_DELETED.getId())) {
            entityStatuses.add(DELETED);
        }

        if (permissionService.getPermissionsFromContext(PENALTY).contains(PENALTY_VIEW_BASIC.getId())) {
            entityStatuses.add(EntityStatus.ACTIVE);
        }
        return entityStatuses;
    }

    /**
     * Returns the penalty info with the given id
     *
     * @param id of the penalty to be returned
     * @return Response dto for created penalty entity
     * @throws DomainEntityNotFoundException if penalty or penalty term not found for given id
     */
    public PenaltyResponse getById(Long id) {
        log.debug("Fetching Penalty for id : {}", id);
        var penalty = penaltyRepository
                .findByIdAndStatusIn(id, getStatusesByPermissions())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find penalty with Id : %s;".formatted(id)));

        var paymentTerm = penaltyPaymentTermRepository
                .findByPenaltyIdAndStatus(penalty.getId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find payment term for penalty : %s;".formatted(penalty.getId())));

        PenaltyResponse penaltyResponse = getPenaltyResponse(penalty, paymentTerm);
        penaltyResponse.setIsLocked(penaltyRepository.hasLockedConnection(id));
        contractTemplateRepository.findTemplateResponseById(penalty.getTemplateId(), LocalDate.now()).ifPresent(penaltyResponse::setTemplateResponse);
        contractTemplateRepository.findTemplateResponseById(penalty.getEmailTemplateId(), LocalDate.now()).ifPresent(penaltyResponse::setEmailTemplateResponse);
        return penaltyResponse;
    }


    /**
     * Creates the penalty for the given request
     *
     * @param request The request based on which the penalty will be created
     * @return Response dto for created penalty entity
     */
    @Transactional
    public PenaltyResponse create(PenaltyRequest request) {
        log.debug("Creating penalty with request : {}", request);

        validateRequestOnCreate(request);
        var penalty = penaltyDataMapper.fromRequest(request);
        //TODO TEMPLATE for delivery purpose - should be removed
        penalty.setTemplateId(validateAndSetTemplate(request.getTemplateId(),ContractTemplateType.DOCUMENT,"templateId-Template with id %s does not exist or has different purpose!;"));
        penalty.setEmailTemplateId(validateAndSetTemplate(request.getEmailTemplateId(),ContractTemplateType.EMAIL,"emailTemplateId-Template with id %s does not exist or has different purpose!;"));
        penalty = penaltyRepository.saveAndFlush(penalty);

        var penaltyPaymentTerm = penaltyDataMapper.fromRequest(penalty.getId(), request.getPenaltyPaymentTermRequest());
        penaltyPaymentTerm = penaltyPaymentTermRepository.save(penaltyPaymentTerm);
        // TODO: 12/17/23 returning full response on create (and edit) is not necessary, only created (or updated) object id is enough

        mapAndSaveActionTypes(request.getActionTypeList(), penalty.getId());
        return getPenaltyResponse(penalty, penaltyPaymentTerm);
    }

    private void mapAndSaveActionTypes(List<Long> actionTypeList, Long penaltyId) {
        List<PenaltyActionTypes> penaltyActionTypes = penaltyDataMapper.toPenaltyActionTypeList(actionTypeList, penaltyId);
        penaltyActionTypesRepository.saveAll(penaltyActionTypes);
    }

    /**
     * Edits the penalty with the given id for the given request
     *
     * @param id      of the penalty to be updated
     * @param request data for penalty update
     * @return Response dto for created penalty entity
     * @throws DomainEntityNotFoundException if penalty or penalty term not found for given id
     */
    @Transactional
    public PenaltyResponse edit(Long id, PenaltyRequest request) {
        log.debug("Editing Penalty for id : {} with request : {}", id, request);

        if (penaltyRepository.hasLockedConnection(id) && !hasEditLockedPermission()) {
            throw new OperationNotAllowedException("You can't edit price component because it is connected to the product contract, service contract or service order!;");
        }

        PenaltyPaymentTermRequest penaltyPaymentTermRequest = request.getPenaltyPaymentTermRequest();

        var penalty = penaltyRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find penalty with Id : %s;".formatted(id)));

        var paymentTerm = penaltyPaymentTermRepository
                .findByPenaltyIdAndStatus(penalty.getId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find payment term  for penalty with id: %s;".formatted(penalty.getId())));
        //TODO TEMPLATE for delivery purpose - should be removed
        if(!Objects.equals(penalty.getTemplateId(),request.getTemplateId())){
            penalty.setTemplateId(validateAndSetTemplate(request.getTemplateId(),ContractTemplateType.DOCUMENT,"templateId-Template with id %s does not exist or has different purpose!;"));
        }
        if(!Objects.equals(penalty.getEmailTemplateId(),request.getEmailTemplateId())){
            penalty.setEmailTemplateId(validateAndSetTemplate(request.getEmailTemplateId(),ContractTemplateType.EMAIL,"emailTemplateId-Template with id %s does not exist or has different purpose!;"));

        }
        validateRequestOnEdit(penalty, paymentTerm, request);

        penaltyDataMapper.updatePenalty(penalty, request);
        penaltyRepository.save(penalty);

        penaltyDataMapper.updatePaymentTerm(paymentTerm, penaltyPaymentTermRequest);
        penaltyPaymentTermRepository.save(paymentTerm);
        updateActionTypes(request.getActionTypeList(), penalty.getId());
        return getPenaltyResponse(penalty, paymentTerm);
    }

    private void updateActionTypes(List<Long> actionTypeListFromReq, Long penaltyId) {
        List<PenaltyActionTypes> allByPenaltyIdAndStatus = penaltyActionTypesRepository.findAllByPenaltyIdAndStatus(penaltyId, EntityStatus.ACTIVE);
        allByPenaltyIdAndStatus.forEach(actionType -> actionType.setStatus(DELETED));
        mapAndSaveActionTypes(actionTypeListFromReq, penaltyId);
    }

    private void validateRequestOnCreate(PenaltyRequest request) {
        validateCurrency(request.getCurrencyId());
        validateCalendar(request.getPenaltyPaymentTermRequest());
        validateActionTypeOnCreate(request.getActionTypeList());
    }

    private void validateActionTypeOnCreate(List<Long> actionTypeListFromReq) {
        List<String> exceptionMessages = new ArrayList<>();
        for (int i = 0; i < actionTypeListFromReq.size(); i++) {
            validateActionType(actionTypeListFromReq.get(i), List.of(ACTIVE), i, exceptionMessages);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
    }

    private void validateActionType(Long actionTypeId, List<NomenclatureItemStatus> statuses, int i, List<String> exceptionMessages) {
        if (!actionTypeRepository.existsByIdAndStatusIn(actionTypeId, statuses)) {
            exceptionMessages.add("actionTypeList[%s]-Action type with ID %s not found in statuses %s;".formatted(i, actionTypeId, statuses));
        }
    }

    private void validateRequestOnEdit(Penalty penalty, PenaltyPaymentTerm paymentTerm, PenaltyRequest request) {
        validateCurrencyOnEdit(penalty, request.getCurrencyId());
        validateCalendarOnEdit(paymentTerm, request.getPenaltyPaymentTermRequest());
        validateActionTypeOnEdit(penalty.getId(), request.getActionTypeList());
    }

    private void validateActionTypeOnEdit(Long penaltyId, List<Long> actionTypeListFromReq) {
        List<String> exceptionMessages = new ArrayList<>();
        List<Long> actionTypeIds = penaltyActionTypesRepository.findAllByPenaltyIdAndStatus(penaltyId, EntityStatus.ACTIVE)
                .stream()
                .map(PenaltyActionTypes::getActionTypeId)
                .toList();
        for (int i = 0; i < actionTypeListFromReq.size(); i++) {
            if (!actionTypeIds.contains(actionTypeListFromReq.get(i))) {
                validateActionType(actionTypeListFromReq.get(i), List.of(ACTIVE), i, exceptionMessages);
            } else {
                validateActionType(actionTypeListFromReq.get(i), List.of(ACTIVE, INACTIVE), i, exceptionMessages);
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
    }

    private void validateCalendar(PenaltyPaymentTermRequest penaltyPaymentTermRequest) {
        if (!calendarRepository.existsByIdAndStatus(penaltyPaymentTermRequest.getCalendarId(), ACTIVE)) {
            log.error("Error while fetching calendar with id: {}", penaltyPaymentTermRequest.getCalendarId());
            throw new DomainEntityNotFoundException("penaltyPaymentTermRequest.calendarId-Unable to find calendar with Id : %s;"
                    .formatted(penaltyPaymentTermRequest.getCalendarId()));
        }
    }

    private void validateCalendarOnEdit(PenaltyPaymentTerm penaltyPaymentTerm, PenaltyPaymentTermRequest request) {
        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(request.getCalendarId(), List.of(ACTIVE, INACTIVE));
        if (calendarOptional.isEmpty()) {
            log.error("Error while fetching calendar with id: {}", request.getCalendarId());
            throw new DomainEntityNotFoundException("penaltyPaymentTermRequest.calendarId-Unable to find calendar with Id : %s;"
                    .formatted(penaltyPaymentTerm.getCalendarId()));
        } else {
            Calendar calendar = calendarOptional.get();
            if (calendar.getStatus().equals(INACTIVE)) {
                if (!Objects.equals(penaltyPaymentTerm.getCalendarId(), request.getCalendarId())) {
                    throw new OperationNotAllowedException("penaltyPaymentTermRequest.calendarId-Unable to assign INACTIVE calendar with Id : %s;"
                            .formatted(request.getCalendarId()));
                }
            }
        }
    }

    private void validateCurrency(Long currencyId) {
        if (currencyId != null && !currencyRepository.existsByIdAndStatus(currencyId, ACTIVE)) {
            log.error("Error while fetching currency for id : {}", currencyId);
            throw new DomainEntityNotFoundException("currencyId-Currency not found with id : %s;".formatted(currencyId));
        }
    }

    private void validateCurrencyOnEdit(Penalty penalty, Long currencyId) {
        if (currencyId == null) {
            return;
        }
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(ACTIVE, INACTIVE));
        if (currencyOptional.isEmpty()) {
            log.error("Error while fetching currency for id : {}", currencyId);
            throw new DomainEntityNotFoundException("currencyId-Currency not found with id : %s;".formatted(currencyId));
        } else {
            Currency currency = currencyOptional.get();
            if (currency.getStatus().equals(INACTIVE)) {
                if (!Objects.equals(penalty.getCurrencyId(), currency.getId())) {
                    throw new OperationNotAllowedException("currencyId-Unable to assign INACTIVE Currency with id : %s;".formatted(currencyId));
                }
            }
        }
    }

    private PenaltyResponse getPenaltyResponse(Penalty penalty, PenaltyPaymentTerm paymentTerm) {
        // TODO: 12/17/23 fetching penalty response needs optimization. Instead of getting all fields by separate queries, collect them in one query

        String currencyName = null;
        if (penalty.getCurrencyId() != null) {
            currencyName = currencyRepository.getReferenceById(penalty.getCurrencyId()).getName();
        }
        var penaltyResponse = penaltyDataMapper.toResponse(penalty, currencyName);
        if (paymentTerm != null) {
            Calendar calendar = calendarRepository.getReferenceById(paymentTerm.getCalendarId());
            penaltyResponse.setPaymentTerm(penaltyDataMapper.toResponse(paymentTerm, calendar));
        }

        setFormulaVariablesInfoToResponse(penalty, penaltyResponse);
        setActionTypesInfoToResponse(penaltyResponse, penalty.getId());
        contractTemplateRepository.findTemplateResponseById(penalty.getTemplateId(), LocalDate.now()).ifPresent(penaltyResponse::setTemplateResponse);
        return penaltyResponse;
    }

    private void setActionTypesInfoToResponse(PenaltyResponse penaltyResponse, Long penaltyId) {
        List<PenaltyActionTypes> actionTypes = penaltyActionTypesRepository.findAllByPenaltyIdAndStatus(penaltyId, EntityStatus.ACTIVE);
        List<PenaltyActionTypeResponse> actionTypeResponses = actionTypes.stream().map(actionType -> PenaltyActionTypeResponse.builder()
                .id(actionType.getActionTypeId())
                .name(actionTypeRepository.getReferenceById(actionType.getActionTypeId()).getName())
                .build()).toList();
        penaltyResponse.setActionTypeResponses(actionTypeResponses);
    }


    private void setFormulaVariablesInfoToResponse(Penalty penalty, PenaltyResponse penaltyResponse) {
        if (StringUtils.isNotEmpty(penalty.getAmountCalculationFormula())) {
            PenaltyFormulaVariablesExtractionResult extractionResult = PenaltyFormulaVariablesExtractionResult.extractVariables(penalty.getAmountCalculationFormula());
            processPriceParameterVariables(penaltyResponse, extractionResult.getPriceParameterVariables());
            processPriceComponentTagVariables(penaltyResponse, extractionResult.getPriceComponentTagVariables());
        }
    }

    private void processPriceParameterVariables(PenaltyResponse penaltyResponse, Set<String> priceParameterVariables) {
        if (CollectionUtils.isNotEmpty(priceParameterVariables)) {
            List<PenaltyVariableResponse> priceParameterVariablesInfo = new ArrayList<>();

            for (String variableName : priceParameterVariables) {
                long priceParameterId;
                try {
                    priceParameterId = Long.parseLong(variableName.substring(PenaltyFormulaVariable.getPriceParameterVariablePrefix().length()));
                } catch (NumberFormatException e) {
                    // this should not lead to exception, logging is sufficient
                    log.error("Unable to parse price parameter id from variable: {}", variableName);
                    continue;
                }

                // status of price parameter is deliberately not checked here, because a price parameter can be deleted,
                // but still used in a penalty formula (will be handled in penalty calculation in action)
                String latestVersionName = priceParameterDetailsRepository.findNameOfLatestVersionByParameterId(priceParameterId);

                priceParameterVariablesInfo.add(new PenaltyVariableResponse(priceParameterId, latestVersionName, variableName));
            }

            penaltyResponse.setPriceParameterVariablesInfo(priceParameterVariablesInfo);
        }
    }

    private void processPriceComponentTagVariables(PenaltyResponse penaltyResponse, Set<String> priceComponentTagVariables) {
        if (CollectionUtils.isNotEmpty(priceComponentTagVariables)) {
            List<PenaltyVariableResponse> priceParameterVariablesInfo = new ArrayList<>();

            for (String variableName : priceComponentTagVariables) {
                String priceComponentTag = variableName.substring(PenaltyFormulaVariable.getPriceComponentTagVariablePrefix().length() - 1);
                priceParameterVariablesInfo.add(new PenaltyVariableResponse(variableName, priceComponentTag));
            }
            penaltyResponse.setPriceComponentTagVariablesInfo(priceParameterVariablesInfo);
        }
    }


    /**
     * Deletes the penalty with the given id if all the conditions are met
     *
     * @param id of the penalty to be updated
     * @throws DomainEntityNotFoundException if penalty or found for given id
     */
    @Transactional
    public Long delete(Long id) {
        var penalty = penaltyRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find penalty with Id : %s;".formatted(id)));

        if (penalty.getStatus().equals(DELETED)) {
            log.error("id-Penalty already deleted for id : {}", id);
            throw new ClientException("id-Penalty already deleted for id : %s;".formatted(id), OPERATION_NOT_ALLOWED);
        }

        if (penalty.getPenaltyGroupDetailId() != null) {
            log.error("id-You can’t delete the penalty with ID [%s] because it is connected to the group of penalties;".formatted(id));
            throw new ClientException("id-You can’t delete the penalty with ID [%s] because it is connected to the group of penalties;".formatted(id), OPERATION_NOT_ALLOWED);
        }

        if (penaltyRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE), List.of(ProductSubObjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the penalty with ID [%s] because it is connected to the product;".formatted(id));
            throw new ClientException("id-You can’t delete the penalty with ID [%s] because it is connected to the product;".formatted(id), OPERATION_NOT_ALLOWED);
        }

        if (penaltyRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE), List.of(ServiceSubobjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the penalty with ID [%s] because it is connected to the service;".formatted(id));
            throw new ClientException("id-You can’t delete the penalty with ID [%s] because it is connected to the service;".formatted(id), OPERATION_NOT_ALLOWED);
        }

        penalty.setStatus(DELETED);
        penaltyRepository.save(penalty);
        return penalty.getId();
    }


    @Override
    public CopyDomain getDomain() {
        return CopyDomain.PENALTIES;
    }

    @Override
    public Page<CopyDomainListResponse> filterCopyDomain(CopyDomainBaseRequest request) {
        request.setPrompt(request.getPrompt() == null ? null : request.getPrompt().trim());
        Sort.Order order = new Sort.Order(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(order));
        return penaltyRepository.filterForCopy(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                List.of(EntityStatus.ACTIVE),
                pageable);
    }


    /**
     * Returns penalty response for copying purposes with active nomenclatures only.
     * If payment term contains inactive nomenclature, it will still be copied without that field.
     *
     * @param id id of the penalty to be copied
     * @return {@link PenaltyResponse} object with active nomenclatures only
     */
    public PenaltyResponse viewForCopy(Long id) {
        Penalty penalty = penaltyRepository
                .findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Penalty not found with id: %s;".formatted(id)));

        PenaltyPaymentTerm paymentTerm = penaltyPaymentTermRepository
                .findByPenaltyIdAndStatus(penalty.getId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find payment term  for penalty with id: %s;".formatted(penalty.getId())));

        boolean isValidCurrency = penalty.getCurrencyId() != null && currencyRepository.existsByIdAndStatusIn(penalty.getCurrencyId(), List.of(ACTIVE));
        boolean isValidCalendar = paymentTerm.getCalendarId() != null && calendarRepository.existsByIdAndStatusIn(paymentTerm.getCalendarId(), List.of(ACTIVE));

        String currencyName = null;
        if (isValidCurrency) {
            currencyName = currencyRepository.getReferenceById(penalty.getCurrencyId()).getName();
        }

        Calendar calendar = null;
        if (isValidCalendar) {
            calendar = calendarRepository.getReferenceById(paymentTerm.getCalendarId());
        }
        PenaltyResponse penaltyResponse = penaltyDataMapper.toResponse(penalty, currencyName);
        if (!isValidCurrency) {
            penaltyResponse.setCurrencyId(null);
        }

        PenaltyPaymentTermsCopyResponse paymentTermResponse = penaltyDataMapper.toResponse(paymentTerm, calendar);
        penaltyResponse.setPaymentTerm(paymentTermResponse);
        setFormulaVariablesInfoToResponse(penalty, penaltyResponse);
        copyActionTypesInfo(penaltyResponse, penalty.getId());
        contractTemplateRepository.findTemplateResponseForCopy(penalty.getTemplateId(), LocalDate.now()).ifPresent(penaltyResponse::setTemplateResponse);
        contractTemplateRepository.findTemplateResponseForCopy(penalty.getEmailTemplateId(), LocalDate.now()).ifPresent(penaltyResponse::setEmailTemplateResponse);
        return penaltyResponse;
    }

    private void copyActionTypesInfo(PenaltyResponse penaltyResponse, Long penaltyId) {
        List<PenaltyActionTypes> penaltyActionTypesList = penaltyActionTypesRepository.findAllByPenaltyIdAndStatus(penaltyId, EntityStatus.ACTIVE);
        List<Long> actionTypesIds = penaltyActionTypesList.stream().map(PenaltyActionTypes::getActionTypeId).toList();
        List<ActionType> activeActionTypes = actionTypeRepository.findAllByIdInAndStatus(actionTypesIds, ACTIVE);
        List<PenaltyActionTypeResponse> actionTypeResponses = activeActionTypes.stream().map(actionType -> PenaltyActionTypeResponse.builder()
                .id(actionType.getId())
                .name(actionType.getName())
                .build()).toList();
        penaltyResponse.setActionTypeResponses(actionTypeResponses);
    }


    /**
     * Returns list of all available penalties filtered by a prompt sorted by creation date desc.
     *
     * @return list of {@link AvailablePenaltyResponse} objects
     */
    public Page<AvailablePenaltyResponse> findAvailable(AvailablePenaltySearchRequest request) {
        log.debug("Retrieving a list of all available penalties by request: {}", request);
        Page<Penalty> penalties = penaltyRepository
                .getAvailablePenaltiesByPrompt(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );

        return penalties.map(AvailablePenaltyResponse::responseFromEntity);
    }


    /**
     * Adds penalties to service details
     *
     * @param penaltyIds`       list of penalty ids to be added
     * @param serviceDetails    service details to which penalties are to be added
     * @param exceptionMessages list of exception messages to be populated in case of any error
     */
    @Transactional
    public void addPenaltiesToService(List<Long> penaltyIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(penaltyIds)) {
            // fetch all available penalties for the service at the moment of adding
            List<Long> availablePenalties = penaltyRepository.findAvailablePenaltyIdsForService(penaltyIds);
            List<ServicePenalty> tempList = new ArrayList<>();

            for (int i = 0; i < penaltyIds.size(); i++) {
                Long penaltyId = penaltyIds.get(i);
                if (availablePenalties.contains(penaltyId)) {
                    Optional<Penalty> penaltyOptional = penaltyRepository.findById(penaltyId);
                    if (penaltyOptional.isEmpty()) {
                        log.error("penalties[%s]-Unable to find penalty with Id : %s;".formatted(i, penaltyId));
                        exceptionMessages.add("additionalSettings.penalties[%s]-Unable to find penalty with Id : %s;".formatted(i, penaltyId));
                        continue;
                    }

                    ServicePenalty servicePenalty = new ServicePenalty();
                    servicePenalty.setServiceDetails(serviceDetails);
                    servicePenalty.setPenalty(penaltyOptional.get());
                    servicePenalty.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(servicePenalty);
                } else {
                    log.error("penalties[%s]-Penalty with Id : %s is not available for service;".formatted(i, penaltyId));
                    exceptionMessages.add("additionalSettings.penalties[%s]-Penalty with Id : %s is not available for service;".formatted(i, penaltyId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            servicePenaltyRepository.saveAll(tempList);
        }
    }


    /**
     * Updates penalties for service details existing version
     *
     * @param requestPenaltyIds list of penalty ids to be added to the service
     * @param serviceDetails    service details to which penalties are to be added
     * @param exceptionMessages list of exception messages to be populated in case of any error
     */
    @Transactional
    public void updateServicePenaltiesForExistingVersion(List<Long> requestPenaltyIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        // fetch all active db penalties for the service
        List<ServicePenalty> dbPenalties = servicePenaltyRepository
                .findAllByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestPenaltyIds)) {
            List<Long> dbPenaltyIds = dbPenalties.stream().map(p -> p.getPenalty().getId()).toList();

            // fetch all available penalties for the service at the moment of adding
            List<Long> availablePenalties = penaltyRepository.findAvailablePenaltyIdsForService(requestPenaltyIds);
            List<ServicePenalty> tempList = new ArrayList<>();

            for (int i = 0; i < requestPenaltyIds.size(); i++) {
                Long pId = requestPenaltyIds.get(i);
                if (!dbPenaltyIds.contains(pId)) { // if penalty is new, its availability should be checked
                    if (availablePenalties.contains(pId)) {
                        Optional<Penalty> penaltyOptional = penaltyRepository.findById(pId);
                        if (penaltyOptional.isEmpty()) {
                            log.error("penalties[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                            exceptionMessages.add("additionalSettings.penalties[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                            continue;
                        }

                        ServicePenalty servicePenalty = new ServicePenalty();
                        servicePenalty.setServiceDetails(serviceDetails);
                        servicePenalty.setPenalty(penaltyOptional.get());
                        servicePenalty.setStatus(ServiceSubobjectStatus.ACTIVE);
                        tempList.add(servicePenalty);
                    } else {
                        log.error("penalties[%s]-Penalty with Id : %s is not available for service;".formatted(i, pId));
                        exceptionMessages.add("additionalSettings.penalties[%s]-Penalty with Id : %s is not available for service;".formatted(i, pId));
                    }
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            servicePenaltyRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPenalties)) {
            for (ServicePenalty sp : dbPenalties) {
                // if user has removed penalties, set DELETED status
                if (!requestPenaltyIds.contains(sp.getPenalty().getId())) {
                    sp.setStatus(ServiceSubobjectStatus.DELETED);
                    servicePenaltyRepository.save(sp);
                }
            }
        }
    }


    /**
     * Updates penalties for service details new version
     *
     * @param requestPenaltyIds    list of penalty ids to be added to the service
     * @param updatedServiceDetail updated service details (new version)
     * @param sourceServiceDetail  source service details (version from which new version was created)
     * @param exceptionMessages    list of exception messages to be populated in case of any error
     */
    @Transactional
    public void updateServicePenaltiesForNewVersion(List<Long> requestPenaltyIds,
                                                    ServiceDetails updatedServiceDetail,
                                                    ServiceDetails sourceServiceDetail,
                                                    List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(requestPenaltyIds)) {
            // fetch all active db penalties from the source version
            List<ServicePenalty> dbPenalties = servicePenaltyRepository
                    .findAllByServiceDetailsIdAndStatusIn(sourceServiceDetail.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            List<Long> dbPenaltyIds = dbPenalties.stream().map(p -> p.getPenalty().getId()).toList();

            // fetch all available penalties for the service at the moment of adding
            List<Long> availablePenalties = penaltyRepository.findAvailablePenaltyIdsForService(requestPenaltyIds);
            List<Penalty> tempList = new ArrayList<>();

            for (int i = 0; i < requestPenaltyIds.size(); i++) {
                Long pId = requestPenaltyIds.get(i);
                if (dbPenaltyIds.contains(pId)) { // if penalty is from the source version, it should be cloned
                    Penalty sourcePenalty = dbPenalties.stream()
                            .filter(p -> p.getPenalty().getId().equals(pId))
                            .findFirst().get().getPenalty();
                    Penalty cloned = clonePenalty(sourcePenalty.getId());
                    tempList.add(cloned);
                } else {
                    if (availablePenalties.contains(pId)) { // if penalty is new, its availability should be checked
                        Optional<Penalty> penaltyOptional = penaltyRepository.findById(pId);
                        if (penaltyOptional.isEmpty()) {
                            log.error("penalties[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                            exceptionMessages.add("additionalSettings.penalties[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                            continue;
                        }

                        tempList.add(penaltyOptional.get());
                    } else {
                        log.error("penalties[%s]-Penalty with Id : %s is not available for service;".formatted(i, pId));
                        exceptionMessages.add("additionalSettings.penalties[%s]-Penalty with Id : %s is not available for service;".formatted(i, pId));

                    }
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            for (Penalty penalty : tempList) {
                ServicePenalty servicePenalty = new ServicePenalty();
                servicePenalty.setServiceDetails(updatedServiceDetail);
                servicePenalty.setPenalty(penalty);
                servicePenalty.setStatus(ServiceSubobjectStatus.ACTIVE);
                servicePenaltyRepository.save(servicePenalty);
            }
        }
    }

    /**
     * Adds penalties to product details
     *
     * @param penaltyIdsSet     list of penalty ids to be added
     * @param productDetails    product details to which penalties are to be added
     * @param exceptionMessages list of exception messages to be populated in case of any error
     */
    @Transactional
    public void addPenaltiesToProduct(List<Long> penaltyIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(penaltyIdsSet)) {
            List<Long> penaltyIds = new ArrayList<>(penaltyIdsSet); // this is for the sake of getting index of element when handling errors

            // fetch all available penalties for the product at the moment of adding
            List<Long> availablePenalties = penaltyRepository.findAvailablePenaltyIdsForProduct(penaltyIds);
            List<ProductPenalty> tempList = new ArrayList<>();

            for (int i = 0; i < penaltyIds.size(); i++) {
                Long penaltyId = penaltyIds.get(i);
                Optional<Penalty> penaltyOptional = penaltyRepository.findById(penaltyId);
                if (penaltyOptional.isEmpty()) {
                    log.error("penaltyIds[%s]-Unable to find penalty with Id : %s;".formatted(i, penaltyId));
                    exceptionMessages.add("additionalSettings.penaltyIds[%s]-Unable to find penalty with Id : %s;".formatted(i, penaltyId));
                    continue;
                }
                if (availablePenalties.contains(penaltyId)) {
                    ProductPenalty productPenalty = new ProductPenalty();
                    productPenalty.setProductDetails(productDetails);
                    productPenalty.setPenalty(penaltyOptional.get());
                    productPenalty.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(productPenalty);
                } else {
                    log.error("penaltyIds[%s]-Penalty with Id : %s is not available for product;".formatted(i, penaltyId));
                    exceptionMessages.add("additionalSettings.penaltyIds[%s]-Penalty with Id : %s is not available for product;".formatted(i, penaltyId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            productPenaltyRepository.saveAll(tempList);
        }
    }


    /**
     * Updates penalties for product details existing version
     *
     * @param requestPenaltyIdsSet list of penalty ids to be added to the product
     * @param productDetails       product details to which penalties are to be added
     * @param exceptionMessages    list of exception messages to be populated in case of any error
     */
    @Transactional
    public void updateProductPenaltiesForExistingVersion(List<Long> requestPenaltyIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        // fetch all active db penalties for the product
        List<ProductPenalty> dbPenalties = productPenaltyRepository
                .findAllByProductDetailsIdAndProductSubObjectStatusIn(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestPenaltyIdsSet)) {
            List<Long> requestPenaltyIds = new ArrayList<>(requestPenaltyIdsSet); // this is for the sake of getting index of element when handling errors

            List<Long> dbPenaltyIds = dbPenalties.stream().map(p -> p.getPenalty().getId()).toList();

            // fetch all available penalties for the product at the moment of adding
            List<Long> availablePenalties = penaltyRepository.findAvailablePenaltyIdsForProduct(requestPenaltyIds);
            List<ProductPenalty> tempList = new ArrayList<>();

            for (int i = 0; i < requestPenaltyIds.size(); i++) {
                Long pId = requestPenaltyIds.get(i);
                Optional<Penalty> penaltyOptional = penaltyRepository.findById(pId);
                if (penaltyOptional.isEmpty()) {
                    log.error("penaltyIds[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                    exceptionMessages.add("additionalSettings.penaltyIds[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                    continue;
                }
                if (!dbPenaltyIds.contains(pId)) { // if penalty is new, its availability should be checked
                    if (availablePenalties.contains(pId)) {
                        ProductPenalty productPenalty = new ProductPenalty();
                        productPenalty.setProductDetails(productDetails);
                        productPenalty.setPenalty(penaltyOptional.get());
                        productPenalty.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                        tempList.add(productPenalty);
                    } else {
                        log.error("penaltyIds[%s]-Penalty with Id : %s is not available for product;".formatted(i, pId));
                        exceptionMessages.add("additionalSettings.penaltyIds[%s]-Penalty with Id : %s is not available for product;".formatted(i, pId));
                    }
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            productPenaltyRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPenalties)) {
            for (ProductPenalty pp : dbPenalties) {
                // if user has removed penalties, set DELETED status
                if (!requestPenaltyIdsSet.contains(pp.getPenalty().getId())) {
                    pp.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productPenaltyRepository.save(pp);
                }
            }
        }
    }


    /**
     * Updates penalties for product details new version
     *
     * @param requestPenaltyIdsSet list of penalty ids to be added to the product
     * @param updatedProductDetail updated product details (new version)
     * @param sourceProductDetail  source product details (version from which new version was created)
     * @param exceptionMessages    list of exception messages to be populated in case of any error
     */
    @Transactional
    public void updateProductPenaltiesForNewVersion(List<Long> requestPenaltyIdsSet,
                                                    ProductDetails updatedProductDetail,
                                                    ProductDetails sourceProductDetail,
                                                    List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(requestPenaltyIdsSet)) {
            List<Long> requestPenaltyIds = new ArrayList<>(requestPenaltyIdsSet); // this is for the sake of getting index of element when handling errors

            // fetch all active db penalties from the source version
            List<ProductPenalty> dbPenalties = productPenaltyRepository
                    .findAllByProductDetailsIdAndProductSubObjectStatusIn(sourceProductDetail.getId(), List.of(ProductSubObjectStatus.ACTIVE));

            List<Long> dbPenaltyIds = dbPenalties.stream().map(p -> p.getPenalty().getId()).toList();

            // fetch all available penalties for the product at the moment of adding
            List<Long> availablePenalties = penaltyRepository.findAvailablePenaltyIdsForProduct(requestPenaltyIds);
            List<Penalty> tempList = new ArrayList<>();

            for (int i = 0; i < requestPenaltyIds.size(); i++) {
                Long pId = requestPenaltyIds.get(i);
                if (dbPenaltyIds.contains(pId)) { // if penalty is from the source version, it should be cloned
                    Penalty sourcePenalty = dbPenalties.stream()
                            .filter(p -> p.getPenalty().getId().equals(pId))
                            .findFirst().get().getPenalty();
                    Penalty cloned = clonePenalty(sourcePenalty.getId());
                    tempList.add(cloned);
                } else {
                    Optional<Penalty> penaltyOptional = penaltyRepository.findById(pId);
                    if (penaltyOptional.isEmpty()) {
                        log.error("penaltyIds[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                        exceptionMessages.add("additionalSettings.penaltyIds[%s]-Unable to find penalty with Id : %s;".formatted(i, pId));
                        continue;
                    }
                    if (availablePenalties.contains(pId)) { // if penalty is new, its availability should be checked
                        tempList.add(penaltyOptional.get());
                    } else {
                        log.error("penaltyIds[%s]-Penalty with Id : %s is not available for product;".formatted(i, pId));
                        exceptionMessages.add("additionalSettings.penaltyIds[%s]-Penalty with Id : %s is not available for product;".formatted(i, pId));

                    }
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            for (Penalty penalty : tempList) {
                ProductPenalty productPenalty = new ProductPenalty();
                productPenalty.setProductDetails(updatedProductDetail);
                productPenalty.setPenalty(penalty);
                productPenalty.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productPenaltyRepository.save(productPenalty);
            }
        }
    }

    public List<Penalty> clonePenaltiesForSubObjects(List<Penalty> penalties) {
        return copyPenalties(penalties);
    }

    public List<Penalty> copyPenalties(List<Penalty> penaltiesToCopy) {
        List<Penalty> result = new ArrayList<>();
        for (Penalty penalty : penaltiesToCopy) {
            Long currencyId = penalty.getCurrencyId();
            Optional<PenaltyPaymentTerm> penaltyPaymentTermOptional = penaltyPaymentTermRepository.findByPenaltyIdAndStatus(penalty.getId(), EntityStatus.ACTIVE);

            if (currencyId != null) {
                Optional<Currency> byIdAndStatus = currencyRepository.findByIdAndStatus(currencyId, List.of(ACTIVE));
                if (byIdAndStatus.isEmpty()) {
                    continue;
                }
            }

            List<PenaltyActionTypes> penaltyActionTypesList = penaltyActionTypesRepository.findAllByPenaltyIdAndStatus(penalty.getId(), EntityStatus.ACTIVE);
            List<Long> actionTypesIds = penaltyActionTypesList.stream().map(PenaltyActionTypes::getPenaltyId).toList();
            List<ActionType> activeActionTypes = actionTypeRepository.findAllByIdInAndStatus(actionTypesIds, ACTIVE);
            if (activeActionTypes.isEmpty()) {
                continue;
            }

            if (penaltyPaymentTermOptional.isPresent()) {
                PenaltyPaymentTerm penaltyPaymentTerm = penaltyPaymentTermOptional.get();
                if (penaltyPaymentTerm.getCalendarId() != null) {
                    Optional<Calendar> byIdAndStatusIsIn = calendarRepository.findByIdAndStatusIsIn(penaltyPaymentTerm.getCalendarId(), List.of(ACTIVE));
                    if (byIdAndStatusIsIn.isEmpty()) {
                        continue;
                    }
                }

            }

            result.add(clonePenalty(penalty.getId()));
        }
        return result;
    }


    /**
     * Clones penalty with its payment term
     *
     * @param penaltyId source penalty ID
     * @return cloned penalty
     */
    @Transactional
    public Penalty clonePenalty(Long penaltyId) {
        Penalty source = penaltyRepository
                .findById(penaltyId).orElseThrow(() ->
                        new DomainEntityNotFoundException("id-Unable to find penalty with Id : %s;".formatted(penaltyId)));

        Penalty clone = new Penalty();
        clone.setName(source.getName());
        clone.setContractClauseNumber(source.getContractClauseNumber());
        clone.setProcessId(source.getProcessId());
        clone.setProcessStartCode(source.getProcessStartCode());
        clone.setAmountCalculationFormula(source.getAmountCalculationFormula());
        clone.setMinAmount(source.getMinAmount());
        clone.setMaxAmount(source.getMaxAmount());
        clone.setCurrencyId(source.getCurrencyId());
        clone.setAutomaticSubmission(source.isAutomaticSubmission());
        clone.setAdditionalInfo(source.getAdditionalInfo());
        clone.setStatus(EntityStatus.ACTIVE);
        clone.setPartyReceivingPenalties(source.getPartyReceivingPenalties());
        clone.setApplicability(source.getApplicability());
        clone.setPenaltyGroupDetailId(null);
        clone.setNoInterestOnOverdueDebts(source.getNoInterestOnOverdueDebts());
        Penalty clonedPenalty = penaltyRepository.saveAndFlush(clone);

        // source version should always have 1 and only 1 payment term, no need to explicitly check
        PenaltyPaymentTerm paymentTerm = penaltyPaymentTermRepository
                .findByPenaltyIdAndStatusIn(source.getId(), List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Penalty payment term not found by Penalty ID %s;".formatted(source.getId())));

        clonePenaltyPaymentTerm(paymentTerm, clonedPenalty.getId());

        List<PenaltyActionTypes> penaltyActionTypesList = penaltyActionTypesRepository.findAllByPenaltyIdAndStatus(penaltyId, EntityStatus.ACTIVE);
        clonePenaltyActionTypes(penaltyActionTypesList, clonedPenalty.getId());
        penaltyRepository.save(clonedPenalty);
        return clonedPenalty;
    }


    /**
     * Clones a penalty payment term and returns the clone. Cloned penalty payment term is always active.
     * This is one step of penalty cloning process. Cloning penalty should be performed separately.
     *
     * @param source    the penalty payment term to be cloned
     * @param penaltyId owner of the clone
     */
    private PenaltyPaymentTerm clonePenaltyPaymentTerm(PenaltyPaymentTerm source, Long penaltyId) {
        PenaltyPaymentTerm clone = new PenaltyPaymentTerm();
        clone.setCalendarType(source.getCalendarType());
        clone.setValue(source.getValue());
        clone.setValueFrom(source.getValueFrom());
        clone.setValueTo(source.getValueTo());
        clone.setCalendarId(source.getCalendarId());
        clone.setPenaltyId(penaltyId);
        clone.setStatus(EntityStatus.ACTIVE);
        clone.setDueDateChange(source.getDueDateChange());
        clone.setName(source.getName());
        clone.setPenaltyPaymentTermExcludes(source.getPenaltyPaymentTermExcludes());
        return penaltyPaymentTermRepository.saveAndFlush(clone);
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.PENALTY, List.of(PermissionEnum.PENALTY_EDIT_LOCKED));
    }

    private void clonePenaltyActionTypes(List<PenaltyActionTypes> source, Long clonedPenaltyId) {
        List<PenaltyActionTypes> actionTypes = source.stream()
                .map(sourceObj -> PenaltyActionTypes.builder()
                        .actionTypeId(sourceObj.getActionTypeId())
                        .penaltyId(clonedPenaltyId)
                        .status(EntityStatus.ACTIVE)
                        .build())
                .toList();
        penaltyActionTypesRepository.saveAll(actionTypes);
    }


    private Long validateAndSetTemplate(Long id, ContractTemplateType documentType, String errorMessage) {
        if (id == null) {
            return null;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(id, ContractTemplatePurposes.PENALTY, documentType, LocalDate.now())) {
            throw new DomainEntityNotFoundException(errorMessage.formatted(id));
        }
        return id;
    }
}
