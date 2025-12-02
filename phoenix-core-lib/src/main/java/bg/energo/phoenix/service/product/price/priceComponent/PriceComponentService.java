package bg.energo.phoenix.service.product.price.priceComponent;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.PriceComponentExpressionHolder;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentPriceType;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentValueType;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.price.priceComponent.ProfileForBalancing;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductPriceComponents;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServicePriceComponent;
import bg.energo.phoenix.model.enums.copy.domain.CopyDomain;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.*;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelStatus;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.request.copy.domain.CopyDomainBaseRequest;
import bg.energo.phoenix.model.request.product.price.RuleRequest;
import bg.energo.phoenix.model.request.product.price.priceComponent.*;
import bg.energo.phoenix.model.request.product.product.BalancingNamesRequest;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.ScalesResponse;
import bg.energo.phoenix.model.response.priceComponent.*;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.CcyRestrictionResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ProfileResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.VolumesByScaleResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.VolumesBySettlementPeriodResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.PriceComponentPriceTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.PriceComponentValueTypeRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.price.applicationModel.ApplicationModelRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.ProfileForBalancingRepository;
import bg.energo.phoenix.repository.product.product.ProductForBalancingRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServicePriceComponentRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.domain.CopyDomainBaseService;
import bg.energo.phoenix.service.product.price.priceComponent.applicationModel.ApplicationModelService;
import bg.energo.phoenix.service.product.price.priceComponentGroup.PriceComponentGroupMapper;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.permissions.PermissionEnum.PRICE_COMPONENT_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.PRICE_COMPONENT_VIEW_DELETED;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceComponentService implements CopyDomainBaseService {
    private final PriceComponentRepository priceComponentRepository;
    private final PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository;
    private final VatRateRepository vatRateRepository;
    private final CurrencyRepository currencyRepository;
    private final PriceComponentValueTypeRepository valueTypeRepository;
    private final PriceComponentPriceTypeRepository priceComponentPriceTypeRepository;
    private final ExpressionStringParser expressionStringParser;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final ApplicationModelService applicationModelService;
    private final PriceComponentMapper priceComponentMapper;
    private final PermissionService permissionService;
    private final ServicePriceComponentRepository servicePriceComponentRepository;
    private final ProductPriceComponentRepository productPriceComponentRepository;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentGroupMapper priceComponentGroupMapper;
    private final ProductForBalancingRepository productForBalancingRepository;
    private final ProfileForBalancingRepository profileForBalancingRepository;
    private final InterimAdvancePaymentRepository interimAdvancePaymentRepository;

    /**
     * Creates Price component entity, Validates formula and attaches required application model.
     *
     * @param request PriceComponentRequest
     */
    @Transactional
    public Long create(PriceComponentRequest request) {
        log.debug("Creating price component with request: {}", request);

        List<String> errorMessages = new ArrayList<>();

        if (StringUtils.isNotBlank(request.getFormulaRequest().getCondition())) {
            if (priceComponentMapper.validateConditionLogicalOperatorType(request.getFormulaRequest().getCondition(), errorMessages)
                && priceComponentMapper.validateConditionNotOperatorType(request.getFormulaRequest().getCondition(), errorMessages)
            ) {
                validateCondition(
                        expressionStringParser.replaceParams(request.getFormulaRequest().getCondition()),
                        getConditionVariables(request.getFormulaRequest().getCondition()),
                        errorMessages
                );
            }
        }

        PriceComponent priceComponent = new PriceComponent(request);
        processVatRate(request.getVatRateId(), request.getGlobalVatRate(), priceComponent, List.of(ACTIVE), errorMessages);
        processCurrency(request.getCurrencyId(), priceComponent, List.of(ACTIVE), errorMessages);
        processValueType(request.getPriceComponentValueTypeId(), priceComponent, List.of(ACTIVE), errorMessages);
        processPriceType(request.getPriceComponentPriceTypeId(), priceComponent, List.of(ACTIVE), errorMessages);
        setFormulaData(priceComponent, request.getFormulaRequest(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        priceComponentRepository.saveAndFlush(priceComponent);

        applicationModelService.create(priceComponent, request.getApplicationModelRequest());

        return priceComponent.getId();
    }


    private void processVatRate(Long vatRateId, Boolean globalVatRate, PriceComponent priceComponent, List<NomenclatureItemStatus> statuses, List<String> errorMessages) {
        if (Boolean.FALSE.equals(globalVatRate)) {
            Optional<VatRate> vatRateOptional = vatRateRepository.findByIdAndStatus(vatRateId, statuses);
            if (vatRateOptional.isEmpty()) {
                log.error("vatRateId-Vat rate not found with ID %s;".formatted(vatRateId));
                errorMessages.add("vatRateId-Vat rate not found with ID %s;".formatted(vatRateId));
            } else {
                priceComponent.setVatRate(vatRateOptional.get());
                priceComponent.setGlobalVatRate(false);
            }
        } else {
            Optional<VatRate> globalVatRateOptional = vatRateRepository.findGlobalVatRate(LocalDate.now(), PageRequest.of(0, 1));
            if (globalVatRateOptional.isPresent()) {
                priceComponent.setGlobalVatRate(true);
            } else {
                errorMessages.add("globalVatRate-Respective Global Vat Rate does not exists;");
            }
        }
    }


    private void processCurrency(Long currencyId, PriceComponent priceComponent, List<NomenclatureItemStatus> statuses, List<String> errorMessages) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, statuses);
        if (currencyOptional.isEmpty()) {
            log.error("currencyId-Currency not found with ID %s;".formatted(currencyId));
            errorMessages.add("currencyId-Currency not found with ID %s;".formatted(currencyId));
        } else {
            priceComponent.setCurrency(currencyOptional.get());
        }
    }


    private void processValueType(Long valueTypeId, PriceComponent priceComponent, List<NomenclatureItemStatus> statuses, List<String> errorMessages) {
        Optional<PriceComponentValueType> valueTypeOptional = valueTypeRepository.findByIdAndStatuses(valueTypeId, statuses);
        if (valueTypeOptional.isEmpty()) {
            log.error("priceComponentValueTypeId-Value type not found with ID %s;".formatted(valueTypeId));
            errorMessages.add("priceComponentValueTypeId-Value type not found with ID %s;".formatted(valueTypeId));
        } else {
            priceComponent.setValueType(valueTypeOptional.get());
        }
    }


    private void processPriceType(Long priceTypeId, PriceComponent priceComponent, List<NomenclatureItemStatus> statuses, List<String> errorMessages) {
        Optional<PriceComponentPriceType> priceTypeOptional = priceComponentPriceTypeRepository.findByIdAndStatus(priceTypeId, statuses);
        if (priceTypeOptional.isEmpty()) {
            log.error("priceComponentPriceTypeId-Price type not found with ID %s;".formatted(priceTypeId));
            errorMessages.add("priceComponentPriceTypeId-Price type not found with ID %s;".formatted(priceTypeId));
        } else {
            priceComponent.setPriceType(priceTypeOptional.get());
        }
    }


    /**
     * Validates formula request and updates Price component entity.
     *
     * @param priceComponent PriceComponent entity to be updated
     * @param formulaRequest PriceComponentFormulaRequest with formula data
     */
    private void setFormulaData(PriceComponent priceComponent, PriceComponentFormulaRequest formulaRequest, List<String> errorMessages) {
        if (validateFormula(formulaRequest, errorMessages)) {
            priceComponent.setConditions(formulaRequest.getCondition());
            priceComponent.setPriceFormula(formulaRequest.getExpression());
            priceComponent.setPriceInWords(formulaRequest.getPriceText());
            priceComponent.setIssuedSeparateInvoice(formulaRequest.getIssuedSeparateInvoice());
            priceComponent.setFormulaVariables(getFormulaVariables(priceComponent, formulaRequest.getVariables(), errorMessages));
        } else {
            log.error("formulaRequest-Formula data not valid;");
            errorMessages.add("formulaRequest-Formula data not valid;");
        }
    }


    /**
     * Validates formula request and updates Price component entity.
     *
     * @param priceComponent PriceComponent entity to be updated
     * @param formulaRequest PriceComponentFormulaRequest with formula data
     */
    private void editFormulaData(PriceComponent priceComponent, PriceComponentFormulaRequest formulaRequest, List<String> errorMessages) {
        if (validateFormula(formulaRequest, errorMessages)) {
            priceComponent.setConditions(formulaRequest.getCondition());
            priceComponent.setPriceFormula(formulaRequest.getExpression());
            priceComponent.setPriceInWords(formulaRequest.getPriceText());
            priceComponent.setPriceInWords(formulaRequest.getPriceText());
            priceComponent.setIssuedSeparateInvoice(formulaRequest.getIssuedSeparateInvoice());
            priceComponent.setFormulaVariables(editFormulaVariables(priceComponent, formulaRequest.getVariables()));
        } else {
            log.error("formulaRequest-Formula data not valid;");
            errorMessages.add("formulaRequest-Formula data not valid;");
        }
    }

    /**
     * Collects all variables from formula request.
     *
     * @param priceComponent PriceComponent entity to which formula is attached
     * @param variables      List of PriceComponentFormulaVariableRequest
     * @return List of PriceComponentFormulaVariable
     */
    private List<PriceComponentFormulaVariable> getFormulaVariables(PriceComponent priceComponent, List<PriceComponentFormulaVariableRequest> variables, List<String> errorMessages) {
        List<PriceComponentFormulaVariable> validatedVariables = new ArrayList<>();

        if (CollectionUtils.isEmpty(variables)) {
            return Collections.emptyList();
        }

        boolean inGroup = priceComponent.getPriceComponentGroupDetailId() != null;
        for (int i = 0; i < variables.size(); i++) {
            PriceComponentFormulaVariableRequest variable = variables.get(i);
            if (inGroup) {
                if (variable.getValueTo() != null && variable.getValueFrom() != null) {
                    throw new ClientException("formulaRequest-Value to and ValueFrom should be disabled for variable when in group;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
                }
                if (variable.getValue() == null) {
                    throw new ClientException("formulaRequest-Value should be provided for variable when in group", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
                }
            }

            PriceComponentFormulaVariable priceComponentFormulaVariable = new PriceComponentFormulaVariable();
            priceComponentFormulaVariable.setVariable(PriceComponentMathVariableName.valueOf(variable.getVariable()));
            priceComponentFormulaVariable.setDescription(variable.getDescription());
            priceComponentFormulaVariable.setValue(variable.getValue());
            priceComponentFormulaVariable.setValueFrom(variable.getValueFrom());
            priceComponentFormulaVariable.setValueTo(variable.getValueTo());
            priceComponentFormulaVariable.setPriceComponent(priceComponent);
            priceComponentFormulaVariable.setProfileForBalancing(getProfileForBalancing(variable, i, errorMessages));

            validatedVariables.add(priceComponentFormulaVariable);
        }

        return validatedVariables;
    }

    private PriceComponentFormulaVariable editFormulaVariable(PriceComponentFormulaVariable priceComponentFormulaVariable, PriceComponent priceComponent, PriceComponentFormulaVariableRequest variable, int i, List<String> errorMessages) {
        boolean inGroup = priceComponent.getPriceComponentGroupDetailId() != null;
        if (inGroup) {
            if (variable.getValueTo() != null) {
                errorMessages.add("formulaRequest.variables[%s]- [valueTo] should be disabled for variable when in group;".formatted(i));
            }
            if (variable.getValueFrom() != null) {
                errorMessages.add("formulaRequest.variables[%s]- [valueFrom] should be disabled for variable when in group;".formatted(i));
            }
            if (variable.getValue() == null) {
                errorMessages.add("formulaRequest.variables[%s]- [value] should be provided for variable when in group;".formatted(i));
            }
        }
        boolean inIapGroup = interimAdvancePaymentRepository.findByPriceComponentId(priceComponent.getId()).isPresent();
        if (inIapGroup) {
            if (variable.getValue() == null) {
                errorMessages.add("formulaRequest-variables[%s]- [value] should be mandatory, price component is attached to interim advance payment in group;".formatted(i));
            }
            if (variable.getValueFrom() != null) {
                errorMessages.add("formulaRequest-variables[%s]- [valueFrom] should not be provided, price component is attached to interim advance payment in group;".formatted(i));
            }
            if (variable.getValueTo() != null) {
                errorMessages.add("formulaRequest-variables[%s]- [valueTo] should not be provided, price component is attached to interim advance payment in group;".formatted(i));
            }
        }
        priceComponentFormulaVariable.setVariable(PriceComponentMathVariableName.valueOf(variable.getVariable()));
        priceComponentFormulaVariable.setDescription(variable.getDescription());
        priceComponentFormulaVariable.setValue(variable.getValue());
        priceComponentFormulaVariable.setValueFrom(variable.getValueFrom());
        priceComponentFormulaVariable.setValueTo(variable.getValueTo());
        priceComponentFormulaVariable.setPriceComponent(priceComponent);
        priceComponentFormulaVariable.setProfileForBalancing(getProfileForBalancing(variable, i, errorMessages));
        return priceComponentFormulaVariable;
    }

    private ProfileForBalancing getProfileForBalancing(PriceComponentFormulaVariableRequest variable, int index, List<String> errorMessages) {
        if (Objects.nonNull(variable.getBalancingProfileNameId())) {
            Optional<ProfileForBalancing> productForBalancingOptional = profileForBalancingRepository
                    .findByIdAndStatusIn(variable.getBalancingProfileNameId(), List.of(EntityStatus.ACTIVE));

            if (productForBalancingOptional.isEmpty()) {
                errorMessages.add("formulaRequest.variables.balancingProfileNameId[%s]-Balancing Profile Name with presented ID: [%s] not found;".formatted(index, variable.getBalancingProfileNameId()));
            } else {
                return productForBalancingOptional.get();
            }
        }
        return null;
    }

    /**
     * Collects all variables from formula request and attaches them to PriceComponent entity.
     *
     * @param priceComponent PriceComponent entity to which formula is attached
     * @param variables      List of PriceComponentFormulaVariableRequest
     * @return List of PriceComponentFormulaVariable
     */
    private List<PriceComponentFormulaVariable> editFormulaVariables(PriceComponent priceComponent, List<PriceComponentFormulaVariableRequest> variables) {
        List<PriceComponentFormulaVariable> formulaVariablesToSave = new ArrayList<>();
        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository.findAllByPriceComponentIdOrderByIdAsc(priceComponent.getId());
        Map<PriceComponentMathVariableName, PriceComponentFormulaVariable> formulas = priceComponentFormulaVariables.stream().filter(x -> !x.getVariable().equals(PriceComponentMathVariableName.PRICE_PROFILE)).collect(Collectors.toMap(PriceComponentFormulaVariable::getVariable, j -> j));
        List<String> errorMessages = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            PriceComponentFormulaVariableRequest variable = variables.get(i);
            PriceComponentFormulaVariable formulaVariable = formulas.remove(PriceComponentMathVariableName.valueOf(variable.getVariable()));
            if (formulaVariable == null) {
                PriceComponentFormulaVariable priceComponentFormulaVariable = new PriceComponentFormulaVariable();
                editFormulaVariable(priceComponentFormulaVariable, priceComponent, variable, i, errorMessages);
                formulaVariablesToSave.add(priceComponentFormulaVariable);
                continue;
            }
            editFormulaVariable(formulaVariable, priceComponent, variable, i, errorMessages);
            formulaVariablesToSave.add(formulaVariable);
        }
        if (!CollectionUtils.isEmpty(errorMessages)) {
            throw new ClientException(String.join(",", errorMessages), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        //if left delete
        //Delete check should be added here if formula variable is used in contract.
        List<Long> list = formulas.values().stream().map(PriceComponentFormulaVariable::getId).toList();
        if (!CollectionUtils.isEmpty(list)) {
            priceComponentFormulaVariableRepository.deleteAllById(list);
        }

        return priceComponentFormulaVariableRepository.saveAllAndFlush(formulaVariablesToSave);
    }


    /**
     * Validates formula request.
     *
     * @param request FormulaValidationRequest with formula data
     * @return {@link FormulaValidateResponse} with validation result
     */
    public FormulaValidateResponse getFormulaValidationResponse(FormulaValidationRequest request) {
        List<String> errorMessages = new ArrayList<>();
        boolean valid = validateFormula(request, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return new FormulaValidateResponse(valid);
    }

    private boolean validateFormula(FormulaValidationRequest request, List<String> errorMessages) {
        try {
            if (Boolean.FALSE.equals(checkForMultipleConsecutiveExpressions(request)) && expressionStringParser.isValidMultiplySyntax(request.getExpression())) {
                List<PriceComponentExpressionHolder> priceComponentExpressionHolderList = expressionStringParser.parseExpression(request.getExpression());
                MainLogicalOperator prevLogicalOperators = null;
                for (PriceComponentExpressionHolder expressionHolder : priceComponentExpressionHolderList) {
                    String startStatement = expressionHolder.getStatement().replaceAll("[()]", "");
                    if (startStatement.contains("$$")) {
                        return false;
                    }
                    if (expressionHolder.getCondition() != null) {
                        String startCondition = expressionHolder.getCondition().replaceAll("[()]", "");
                        if (startCondition.contains("$$")) {
                            return false;
                        }
                    }

                    var currentLogicalOperator = expressionHolder.getOperator();
                    if (currentLogicalOperator != null && (currentLogicalOperator.equals(MainLogicalOperator.ELSEIF)
                            || currentLogicalOperator.equals(MainLogicalOperator.ELSE))) {
                        if (prevLogicalOperators != null && prevLogicalOperators.equals(MainLogicalOperator.ELSE)) {
                            log.error("Error in If else order");
                            return false;
                        }
                    }
                    if (!MainLogicalOperator.ELSE.equals(currentLogicalOperator)) {
                        String condition = expressionHolder.getCondition();
                        if (StringUtils.isNotBlank(condition)) {
                            var variables = getVariablesFromString(condition);
                            validateCondition(condition, variables, errorMessages);
                        }
                    }
                    prevLogicalOperators = currentLogicalOperator;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new IllegalArgumentException("formulaRequest.expression- [expression] Expression is not valid;");
        }
    }

    private Boolean checkForMultipleConsecutiveExpressions(FormulaValidationRequest request) {
        String expression = request.getExpression();
        String pattern = "\\+\\+|--|==|ANDAND|OROR|NOTNOT|inin|-\\+|\\+-";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(expression);
        return matcher.find();
    }


    /**
     * Validates condition (used in formula).
     *
     * @param condition Condition to be validated
     * @param variables List of variables used in condition
     */
    private void validateCondition(String condition, Map<String, Object> variables, List<String> errorMessages) {
        try {
            RuleRequest ruleRequest = new RuleRequest();
            ruleRequest.setExpression(condition);
            ruleRequest.setArguments(variables);
            ruleEvaluatorService.evaluateBooleanExpression(ruleRequest);
        } catch (Exception e) {
            log.error("Error while editing Price Component Formula validateCondition ", e);
            errorMessages.add("formulaRequest.condition- [condition] Error while editing Price Component Formula validateCondition;");
        }
    }


    /**
     * Returns all variables used in condition.
     *
     * @param condition String representation of condition
     * @return Map of variables
     */
    private Map<String, Object> getVariablesFromString(String condition) {
        var variables = new HashSet<>(ExpressionStringParser.extractVariableNames(condition));
        return variables.stream().collect(Collectors.toMap(o -> String.format("$%s$", o), o -> 1));
    }


    /**
     * Returns all variables used in condition.
     *
     * @param condition String representation of condition
     * @return Map of variables
     */
    private Map<String, Object> getConditionVariables(String condition) {
        if (StringUtils.isBlank(condition)) {
            return new HashMap<>();
        }
        var variables = expressionStringParser.getVariables(condition);
        Map<String, Object> result = new HashMap<>();
        for (String variable : variables) {
            var conditionVariableName = Arrays.stream(PriceComponentConditionVariableName.values())
                    .filter(pcvn -> variable.contains(pcvn.name())).findAny();
            if (conditionVariableName.isPresent()) {
                switch (conditionVariableName.get().getType()) {
                    case NUMBER -> {
                        result.put("$%s$".formatted(variable), "%s".formatted("811"));
                    }
                    case STRING -> {
                        result.put("$%s$".formatted(variable), "\"%s\"".formatted("PRIVATE_CUSTOMER"));
                    }
                }
            } else {
                var conditionVariableType = Arrays.stream(PriceComponentConditionVariableValue.values())
                        .filter(pcvt -> variable.contains(pcvt.name())).findAny();
                //need to wrap string type parameters with "" so that
                if (conditionVariableType.isPresent()) {
                    result.put("$%s$".formatted(variable), "\"%s\"".formatted(conditionVariableType.get()));
                } else {
                    result.put("$%s$".formatted(variable), "%s".formatted(variable));
                }
            }

        }
        return result;
    }


    /**
     * Returns list of all available price components filtered by a prompt sorted by creation date desc.
     *
     * @param request {@link AvailablePriceComponentSearchRequest
     * @return list of {@link AvailablePriceComponentResponse} objects
     */
    public Page<AvailablePriceComponentResponse> getAvailablePriceComponentsForGroup(AvailablePriceComponentSearchRequest request) {
        log.debug("Retrieving available price components by request: {}", request);

        Page<PriceComponent> availablePriceComponents = priceComponentRepository
                .getAvailablePriceComponentsForGroup(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );

        return availablePriceComponents.map(AvailablePriceComponentResponse::responseFromEntity);
    }


    /**
     * Listing for PriceComponent.
     * PriceComponents are returned based on {@link PriceComponentFilterRequest}.
     *
     * @param request {@link AvailablePriceComponentSearchRequest}
     * @return Page of {@link PriceComponentFilterResponse} objects
     */
    public Page<PriceComponentFilterResponse> filter(PriceComponentFilterRequest request) {
        PriceComponentAvailability availability = request.getAvailability();
        Page<PriceComponentMiddleResponse> filter = priceComponentRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getSearchFields() == null ? null : request.getSearchFields().toString(),
                        request.getValueTypeIds(),
                        request.getPriceTypeIds(),
                        request.getNumberType().stream().map(NumberType::toString).toList(),
                        availability == PriceComponentAvailability.ALL ? null : availability.toString(),
                        request.getConditions().stream().map(Enum::toString).toList(),
                        getStatusesByPermissions().stream().map(Enum::toString).toList(),
                        PageRequest.of(request.getPage(), request.getSize(), Sort.by(request.getSortDirection(), request.getSortBy().getColumn()))
                );
        return filter.map(PriceComponentFilterResponse::new);
    }


    /**
     * Updates price component with given id and request data.
     *
     * @param id      ID of price component to be updated
     * @param request {@link PriceComponentEditRequest} object
     * @return ID of updated price component
     */
    @Transactional
    public Long update(Long id, PriceComponentEditRequest request) {
        log.debug("Updating price component with id: {} and request {}", id, request);

        if (priceComponentRepository.hasLockedConnection(id) && !hasEditLockedPermission()) {
            throw new OperationNotAllowedException("You can't edit price component because it is connected to the product contract, service contract or service order!;");
        }

        List<String> errorMessages = new ArrayList<>();

        if (StringUtils.isNotBlank(request.getFormulaRequest().getCondition())) {
            validateCondition(
                    expressionStringParser.replaceParams(request.getFormulaRequest().getCondition()),
                    getConditionVariables(request.getFormulaRequest().getCondition()),
                    errorMessages
            );
        }

        PriceComponent priceComponent = priceComponentRepository
                .findByIdAndStatusIn(id, List.of(PriceComponentStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-PriceComponent with ID %s not found;".formatted(id)));

        priceComponent.setName(request.getName());
        priceComponent.setInvoiceAndTemplateText(request.getDisplayName());
        priceComponent.setNumberType(request.getNumberType());
        priceComponent.setIncomeAccountNumber(request.getNumberOfIncomeAccount());
        priceComponent.setCostCenterControllingOrder(request.getControllingOrder());
        priceComponent.setContractTemplateTag(request.getTagForContractTemplate());
        priceComponent.setDiscount(request.getDiscount());
        priceComponent.setAlternativeRecipientCustomerDetailId(request.getAlternativeRecipientCustomerDetailId());
        priceComponent.setDoNotIncludeInTheVatBase(Objects.requireNonNullElse(request.getDoNotIncludeVatBase(), false));
        if (!Objects.requireNonNullElse(request.getConsumer(), false) && !Objects.requireNonNullElse(request.getGenerator(), false)) {
            priceComponent.setXenergieApplicationType(null);
        } else {
            priceComponent.setXenergieApplicationType(Objects.requireNonNullElse(request.getConsumer(), false) ? XEnergieApplicationType.CONSUMER : XEnergieApplicationType.GENERATOR);
        }
        processVatRate(
                request.getVatRateId(),
                request.getGlobalVatRate(),
                priceComponent,
                (priceComponent.getVatRate() != null && Objects.equals(priceComponent.getVatRate().getId(), request.getVatRateId())) ? List.of(ACTIVE, INACTIVE) : List.of(ACTIVE),
                errorMessages
        );
        processCurrency(
                request.getCurrencyId(),
                priceComponent,
                Objects.equals(priceComponent.getCurrency().getId(), request.getCurrencyId()) ? List.of(ACTIVE, INACTIVE) : List.of(ACTIVE),
                errorMessages
        );
        processValueType(
                request.getPriceComponentValueTypeId(),
                priceComponent,
                Objects.equals(priceComponent.getValueType().getId(), request.getPriceComponentValueTypeId()) ? List.of(ACTIVE, INACTIVE) : List.of(ACTIVE),
                errorMessages
        );
        processPriceType(
                request.getPriceComponentPriceTypeId(),
                priceComponent,
                Objects.equals(priceComponent.getPriceType().getId(), request.getPriceComponentPriceTypeId()) ? List.of(ACTIVE, INACTIVE) : List.of(ACTIVE),
                errorMessages
        );
        editFormulaData(priceComponent, request.getFormulaRequest(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        priceComponentRepository.save(priceComponent);

        applicationModelService.update(priceComponent.getId(), request.getApplicationModelRequest());

        return priceComponent.getId();
    }


    /**
     * Deletes price component with given id if validations are passed.
     *
     * @param id ID of price component to be deleted
     * @return ID of deleted price component
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting price component with id: {}", id);

        PriceComponent priceComponent = priceComponentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-PriceComponent with ID %s not found;".formatted(id)));

        if (priceComponent.getStatus() == PriceComponentStatus.DELETED) {
            log.error("id-PriceComponent with ID %s is already deleted;".formatted(id));
            throw new ClientException("id-PriceComponent with ID [%s] is already deleted;".formatted(id), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (priceComponent.getPriceComponentGroupDetailId() != null) {
            log.error("id-You can’t delete price component with ID [%s] because it is connected to the group of price components ID [%s]"
                    .formatted(id, priceComponent.getPriceComponentGroupDetailId()));
            throw new ClientException("id-You can’t delete price component with ID [%s] because it is connected to the group of price components ID [%s]"
                    .formatted(id, priceComponent.getPriceComponentGroupDetailId()), ErrorCode.CONFLICT);
        }

        if (priceComponentRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE), List.of(ProductSubObjectStatus.ACTIVE))) {
            log.error("id-You can’t delete price component with ID [%s] because it is connected to the product;".formatted(id));
            throw new ClientException("id-You can’t delete price component with ID [%s] because it is connected to the product;".formatted(id), ErrorCode.CONFLICT);
        }

        if (priceComponentRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE), List.of(ServiceSubobjectStatus.ACTIVE))) {
            log.error("id-You can’t delete price component with ID [%s] because it is connected to the service;".formatted(id));
            throw new ClientException("id-You can’t delete price component with ID [%s] because it is connected to the service;".formatted(id), ErrorCode.CONFLICT);
        }

        if (priceComponentRepository.hasConnectionToInterimAndAdvancePayment(id, List.of(InterimAdvancePaymentStatus.ACTIVE))) {
            log.error("id-You can’t delete price component with ID [%s] because it is connected to the interim and advance payment;".formatted(id));
            throw new ClientException("id-You can’t delete price component with ID [%s] because it is connected to the interim and advance payment;".formatted(id), ErrorCode.CONFLICT);
        }

        priceComponent.setStatus(PriceComponentStatus.DELETED);
        priceComponentRepository.save(priceComponent);

        return priceComponent.getId();
    }


    /**
     * Returns price component with given id.
     *
     * @param id ID of price component to be returned
     * @return {@link PriceComponentDetailedResponse} object
     */
    public PriceComponentDetailedResponse getById(Long id) {
        log.debug("Retrieving price component with id: {}", id);

        var priceComponent = priceComponentRepository
                .findByIdAndStatusIn(id, getStatusesByPermissions())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Unable to find price component with Id : %s;".formatted(id)));

        var applicationModel = applicationModelService.view(priceComponent.getId());

        PriceComponentDetailedResponse detailedResponse = priceComponentMapper.toDetailedResponse(priceComponent, applicationModel);
        detailedResponse.setIsLocked(priceComponentRepository.hasLockedConnection(id));
        return detailedResponse;
    }


    /**
     * Returns list of statuses based on permissions.
     *
     * @return List of {@link PriceComponentStatus} objects
     */
    private List<PriceComponentStatus> getStatusesByPermissions() {
        List<PriceComponentStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.PRICE_COMPONENT);

        if (context.stream().anyMatch(x -> Objects.equals(PRICE_COMPONENT_VIEW_DELETED.getId(), x))) {
            statuses.add(PriceComponentStatus.DELETED);
        }

        if (context.stream().anyMatch(x -> Objects.equals(PRICE_COMPONENT_VIEW_BASIC.getId(), x))) {
            statuses.add(PriceComponentStatus.ACTIVE);
        }
        return statuses;
    }


    /**
     * Returns domain name for price component for copy functionality.
     *
     * @return {@link CopyDomain} object
     */
    @Override
    public CopyDomain getDomain() {
        return CopyDomain.PRICE_COMPONENTS;
    }


    /**
     * Returns list of price components for copy functionality.
     *
     * @param request {@link CopyDomainBaseRequest} object
     * @return {@link Page} of {@link CopyDomainListResponse} objects
     */
    @Override
    public Page<CopyDomainListResponse> filterCopyDomain(CopyDomainBaseRequest request) {
        request.setPrompt(request.getPrompt() == null ? null : request.getPrompt().trim());
        Sort.Order order = new Sort.Order(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(order));
        return priceComponentRepository.filterForCopy(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                List.of(PriceComponentStatus.ACTIVE),
                pageable
        );
    }

    /**
     * Returns price component for copy functionality.
     *
     * @param id ID of price component to be returned
     * @return {@link PriceComponentDetailedResponse} object
     */
    public PriceComponentDetailedResponse viewForCopy(Long id) {
        var priceComponentResponse = getById(id);

        if (priceComponentResponse.getApplicationModelResponse() != null) {
            var applicationModel = priceComponentResponse.getApplicationModelResponse();
            if (applicationModel.getVolumesByScaleResponse() != null) {
                var scales = new ArrayList<ScalesResponse>();
                VolumesByScaleResponse volumesByScaleResponse = applicationModel.getVolumesByScaleResponse();
                volumesByScaleResponse.getScalesResponse()
                        .forEach(scale -> {
                            if (NomenclatureItemStatus.ACTIVE.equals(scale.getStatus())) {
                                scales.add(scale);
                            }
                        });
                volumesByScaleResponse.setScalesResponse(scales);
                List<CcyRestrictionResponse> ccyRestriction = volumesByScaleResponse.getCcyRestriction();
                if (ccyRestriction != null) {
                    volumesByScaleResponse.setCcyRestriction(getCopyOfCcyRestrictions(ccyRestriction));
                }
            }
            if (applicationModel.getVolumesBySettlementPeriodResponse() != null) {
                VolumesBySettlementPeriodResponse volumesBySettlementPeriodResponse = applicationModel.getVolumesBySettlementPeriodResponse();
                List<CcyRestrictionResponse> ccyRestriction = volumesBySettlementPeriodResponse.getCcyRestriction();
                if (ccyRestriction != null) {
                    volumesBySettlementPeriodResponse.setCcyRestriction(getCopyOfCcyRestrictions(ccyRestriction));
                }
            }


            if (applicationModel.getVolumesBySettlementPeriodResponse() != null
                    && applicationModel.getVolumesBySettlementPeriodResponse().getProfileResponses() != null) {
                var profiles = new ArrayList<ProfileResponse>();
                applicationModel.getVolumesBySettlementPeriodResponse().getProfileResponses()
                        .forEach(profile -> {
                            if (NomenclatureItemStatus.ACTIVE.equals(profile.getProfile().getStatus())) {
                                profiles.add(profile);
                            }
                        });
                applicationModel.getVolumesBySettlementPeriodResponse().setProfileResponses(profiles);
            }
        }

        var currencyId = priceComponentResponse.getCurrency().getId();
        var priceTypeId = priceComponentResponse.getPriceType().getId();
        var valueTypeId = priceComponentResponse.getValueType().getId();
        if (priceComponentResponse.getVatRate() != null) {
            var vatRateId = priceComponentResponse.getVatRate().getId();
            var vatRate = vatRateRepository.findByIdAndStatus(vatRateId, List.of(NomenclatureItemStatus.ACTIVE));
            if (vatRate.isEmpty()) {
                priceComponentResponse.setVatRate(null);
            }
        }
        var currency = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
        var priceType = priceComponentPriceTypeRepository.findByIdAndStatus(priceTypeId, List.of(NomenclatureItemStatus.ACTIVE));
        var valueType = valueTypeRepository.findByIdAndStatuses(valueTypeId, List.of(NomenclatureItemStatus.ACTIVE));

        if (currency.isEmpty()) {
            priceComponentResponse.setCurrency(null);
        }
        if (priceType.isEmpty()) {
            priceComponentResponse.setPriceType(null);
        }
        if (valueType.isEmpty()) {
            priceComponentResponse.setValueType(null);
        }

        return priceComponentResponse;
    }


    /**
     * Validates condition in formula.
     *
     * @param request {@link ConditionValidationRequest} object
     * @return {@link FormulaValidateResponse} object
     */
    public FormulaValidateResponse validateCondition(ConditionValidationRequest request) {
        List<String> errorMessages = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCondition())) {
            if (priceComponentMapper.validateConditionLogicalOperatorType(request.getCondition(), errorMessages)
                    && priceComponentMapper.validateConditionNotOperatorType(request.getCondition(), errorMessages)
            ) {
                validateCondition(
                        expressionStringParser.replaceParams(request.getCondition()),
                        getConditionVariables(request.getCondition()),
                        errorMessages
                );
            }
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return new FormulaValidateResponse(true);
    }


    /**
     * Adds price components to service.
     *
     * @param priceComponentIds ids of price components to be added
     * @param serviceDetails    service details to which price components will be added
     * @param exceptionMessages list of exception messages to be filled in case of errors
     */
    @Transactional
    public void addPriceComponentsToService(List<Long> priceComponentIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(priceComponentIds)) {
            // fetch all available price components at the moment of adding
            List<Long> availablePriceComponents = priceComponentRepository.findAvailablePriceComponentIdsForService(priceComponentIds);
            List<ServicePriceComponent> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentIds.size(); i++) {
                Long priceComponentId = priceComponentIds.get(i);
                if (availablePriceComponents.contains(priceComponentId)) {
                    Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
                    if (priceComponentOptional.isEmpty()) {
                        log.error("priceComponents[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                        exceptionMessages.add("priceSettings.priceComponents[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                        continue;
                    }

                    ServicePriceComponent spc = new ServicePriceComponent();
                    spc.setPriceComponent(priceComponentOptional.get());
                    spc.setServiceDetails(serviceDetails);
                    spc.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(spc);
                } else {
                    log.error("priceComponents[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                    exceptionMessages.add("priceSettings.priceComponents[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all price components
            servicePriceComponentRepository.saveAll(tempList);
        }
    }


    /**
     * Updates price components for service existing version.
     *
     * @param priceComponentIds ids of price components to be updated
     * @param serviceDetails    service details to which price components will be updated
     * @param exceptionMessages list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateServicePriceComponentsForExistingVersion(List<Long> priceComponentIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        // fetch all active price components
        List<ServicePriceComponent> dbPriceComponents = servicePriceComponentRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(priceComponentIds)) {
            List<Long> dbPriceComponentIds = dbPriceComponents.stream().map(pc -> pc.getPriceComponent().getId()).toList();

            // fetch all available price components at the moment of adding
            List<Long> availableIAPs = priceComponentRepository.findAvailablePriceComponentIdsForService(priceComponentIds);
            List<ServicePriceComponent> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentIds.size(); i++) {
                Long priceComponentId = priceComponentIds.get(i);
                if (!dbPriceComponentIds.contains(priceComponentId)) { // if price component is new, its availability should be checked
                    if (availableIAPs.contains(priceComponentId)) {
                        Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
                        if (priceComponentOptional.isEmpty()) {
                            log.error("priceComponents[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                            exceptionMessages.add("priceSettings.priceComponents[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                            continue;
                        }

                        ServicePriceComponent spc = new ServicePriceComponent();
                        spc.setPriceComponent(priceComponentOptional.get());
                        spc.setServiceDetails(serviceDetails);
                        spc.setStatus(ServiceSubobjectStatus.ACTIVE);
                        tempList.add(spc);
                    } else {
                        log.error("priceComponents[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                        exceptionMessages.add("priceSettings.priceComponents[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            servicePriceComponentRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPriceComponents)) {
            for (ServicePriceComponent spc : dbPriceComponents) {
                // if user has removed price components, set DELETED status
                if (!priceComponentIds.contains(spc.getPriceComponent().getId())) {
                    spc.setStatus(ServiceSubobjectStatus.DELETED);
                    servicePriceComponentRepository.save(spc);
                }
            }
        }
    }


    /**
     * Updates price components for service new version
     *
     * @param priceComponentIds    ids of price components to be updated
     * @param updatedServiceDetail service details to add price components to
     * @param sourceServiceDetail  source service details (version from which new version was created)
     * @param exceptionMessages    list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateServicePriceComponentsForNewVersion(List<Long> priceComponentIds,
                                                          ServiceDetails updatedServiceDetail,
                                                          ServiceDetails sourceServiceDetail,
                                                          List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(priceComponentIds)) {
            // fetch all active price components from source version
            List<ServicePriceComponent> dbPriceComponents = servicePriceComponentRepository
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetail.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            List<Long> dbPriceComponentIds = dbPriceComponents.stream().map(pc -> pc.getPriceComponent().getId()).toList();

            // fetch all available price components at the moment of adding
            List<Long> availablePriceComponents = priceComponentRepository.findAvailablePriceComponentIdsForService(priceComponentIds);
            List<PriceComponent> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentIds.size(); i++) {
                Long priceComponentId = priceComponentIds.get(i);
                if (dbPriceComponentIds.contains(priceComponentId)) { // if price component is from the source version, it should be cloned
                    PriceComponent sourcePriceComponent = dbPriceComponents.stream()
                            .filter(pc -> pc.getPriceComponent().getId().equals(priceComponentId))
                            .findFirst().get().getPriceComponent(); // will always be present, as we have collected the list above
                    PriceComponent cloned = clonePriceComponent(sourcePriceComponent.getId());
                    tempList.add(cloned);
                } else {
                    if (availablePriceComponents.contains(priceComponentId)) { // if price component is new, its availability should be checked
                        Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
                        if (priceComponentOptional.isEmpty()) {
                            log.error("priceComponents[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                            exceptionMessages.add("priceSettings.priceComponents[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                            continue;
                        }

                        tempList.add(priceComponentOptional.get());
                    } else {
                        log.error("priceComponents[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                        exceptionMessages.add("priceSettings.priceComponents[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            for (PriceComponent item : tempList) {
                ServicePriceComponent serviceInterimAndAdvancePayment = new ServicePriceComponent();
                serviceInterimAndAdvancePayment.setPriceComponent(item);
                serviceInterimAndAdvancePayment.setServiceDetails(updatedServiceDetail);
                serviceInterimAndAdvancePayment.setStatus(ServiceSubobjectStatus.ACTIVE);
                servicePriceComponentRepository.save(serviceInterimAndAdvancePayment);
            }
        }
    }

    /**
     * Adds price components to product.
     *
     * @param priceComponentIdsSet ids of price components to be added
     * @param productDetails       product details to which price components will be added
     * @param exceptionMessages    list of exception messages to be filled in case of errors
     */
    @Transactional
    public void addPriceComponentsToProduct(List<Long> priceComponentIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(priceComponentIdsSet)) {
            List<Long> priceComponentIds = new ArrayList<>(priceComponentIdsSet); // this is for the sake of getting index of element when handling errors
            // fetch all available price components at the moment of adding
            List<Long> availablePriceComponents = priceComponentRepository.findAvailablePriceComponentIdsForProduct(priceComponentIds);
            List<ProductPriceComponents> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentIds.size(); i++) {
                Long priceComponentId = priceComponentIds.get(i);
                Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
                if (priceComponentOptional.isEmpty()) {
                    log.error("priceComponentIds[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                    exceptionMessages.add("priceSettings.priceComponentIds[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                    continue;
                }
                if (availablePriceComponents.contains(priceComponentId)) {
                    ProductPriceComponents productPriceComponents = new ProductPriceComponents();
                    productPriceComponents.setPriceComponent(priceComponentOptional.get());
                    productPriceComponents.setProductDetails(productDetails);
                    productPriceComponents.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(productPriceComponents);
                } else {
                    log.error("priceComponentIds[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                    exceptionMessages.add("priceSettings.priceComponentIds[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all price components
            productPriceComponentRepository.saveAll(tempList);
        }
    }


    /**
     * Updates price components for product existing version.
     *
     * @param priceComponentIdsSet ids of price components to be updated
     * @param productDetails       product details to which price components will be updated
     * @param exceptionMessages    list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateProductPriceComponentsForExistingVersion(List<Long> priceComponentIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        // fetch all active price components
        List<ProductPriceComponents> dbPriceComponents = productPriceComponentRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(priceComponentIdsSet)) {
            List<Long> priceComponentIds = new ArrayList<>(priceComponentIdsSet); // this is for the sake of getting index of element when handling errors
            List<Long> dbPriceComponentIds = dbPriceComponents.stream().map(pc -> pc.getPriceComponent().getId()).toList();

            // fetch all available price components at the moment of adding
            List<Long> availableIAPs = priceComponentRepository.findAvailablePriceComponentIdsForProduct(priceComponentIds);
            List<ProductPriceComponents> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentIds.size(); i++) {
                Long priceComponentId = priceComponentIds.get(i);
                Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
                if (priceComponentOptional.isEmpty()) {
                    log.error("priceComponentIds[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                    exceptionMessages.add("priceSettings.priceComponentIds[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                    continue;
                }
                if (!dbPriceComponentIds.contains(priceComponentId)) { // if price component is new, its availability should be checked
                    if (availableIAPs.contains(priceComponentId)) {
                        ProductPriceComponents productPriceComponents = new ProductPriceComponents();
                        productPriceComponents.setPriceComponent(priceComponentOptional.get());
                        productPriceComponents.setProductDetails(productDetails);
                        productPriceComponents.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                        tempList.add(productPriceComponents);
                    } else {
                        log.error("priceComponentIds[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                        exceptionMessages.add("priceSettings.priceComponentIds[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            productPriceComponentRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPriceComponents)) {
            for (ProductPriceComponents productPriceComponents : dbPriceComponents) {
                // if user has removed price components, set DELETED status
                if (!priceComponentIdsSet.contains(productPriceComponents.getPriceComponent().getId())) {
                    productPriceComponents.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productPriceComponentRepository.save(productPriceComponents);
                }
            }
        }
    }


    /**
     * Updates price components for product new version
     *
     * @param priceComponentIdsSet ids of price components to be updated
     * @param updatedProductDetail product details to add price components to
     * @param sourceProductDetail  source product details (version from which new version was created)
     * @param exceptionMessages    list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateProductPriceComponentsForNewVersion(List<Long> priceComponentIdsSet,
                                                          ProductDetails updatedProductDetail,
                                                          ProductDetails sourceProductDetail,
                                                          List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(priceComponentIdsSet)) {
            List<Long> priceComponentIds = new ArrayList<>(priceComponentIdsSet); // this is for the sake of getting index of element when handling errors
            // fetch all active price components from source version
            List<ProductPriceComponents> dbPriceComponents = productPriceComponentRepository
                    .findByProductDetailsIdAndProductSubObjectStatusIn(sourceProductDetail.getId(), List.of(ProductSubObjectStatus.ACTIVE));

            List<Long> dbPriceComponentIds = dbPriceComponents.stream().map(pc -> pc.getPriceComponent().getId()).toList();

            // fetch all available price components at the moment of adding
            List<Long> availablePriceComponents = priceComponentRepository.findAvailablePriceComponentIdsForProduct(priceComponentIds);
            List<PriceComponent> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentIds.size(); i++) {
                Long priceComponentId = priceComponentIds.get(i);
                if (dbPriceComponentIds.contains(priceComponentId)) { // if price component is from the source version, it should be cloned
                    PriceComponent sourcePriceComponent = dbPriceComponents.stream()
                            .filter(pc -> pc.getPriceComponent().getId().equals(priceComponentId))
                            .findFirst().get().getPriceComponent(); // will always be present, as we have collected the list above
                    PriceComponent cloned = clonePriceComponent(sourcePriceComponent.getId());
                    tempList.add(cloned);
                } else {
                    Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
                    if (priceComponentOptional.isEmpty()) {
                        log.error("priceComponentIds[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                        exceptionMessages.add("priceSettings.priceComponentIds[%s]-can't find price component with id: %s;".formatted(i, priceComponentId));
                        continue;
                    }
                    if (availablePriceComponents.contains(priceComponentId)) { // if price component is new, its availability should be checked
                        tempList.add(priceComponentOptional.get());
                    } else {
                        log.error("priceComponentIds[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                        exceptionMessages.add("priceSettings.priceComponentIds[%s]-Price component with id: %s is not available for adding;".formatted(i, priceComponentId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new Interim Advance Payments
            for (PriceComponent item : tempList) {
                ProductPriceComponents productPriceComponents = new ProductPriceComponents();
                productPriceComponents.setPriceComponent(item);
                productPriceComponents.setProductDetails(updatedProductDetail);
                productPriceComponents.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productPriceComponentRepository.save(productPriceComponents);
            }
        }
    }


    /**
     * Clones price component with formula variables and associated application model (by delegation)
     *
     * @param priceComponentId price component ID to clone
     * @return cloned price component
     */
    @Transactional
    public PriceComponent clonePriceComponent(Long priceComponentId) {
        PriceComponent source = priceComponentRepository
                .findById(priceComponentId)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Price component with ID %s not found;".formatted(priceComponentId)));

        PriceComponent clone = new PriceComponent();
        clone.setName(source.getName());
        clone.setInvoiceAndTemplateText(source.getInvoiceAndTemplateText());
        clone.setPriceType(source.getPriceType());
        clone.setValueType(source.getValueType());
        clone.setCurrency(source.getCurrency());
        clone.setVatRate(source.getVatRate());
        clone.setNumberType(source.getNumberType());
        clone.setGlobalVatRate(source.getGlobalVatRate());
        clone.setDiscount(source.getDiscount());
        clone.setIncomeAccountNumber(source.getIncomeAccountNumber());
        clone.setCostCenterControllingOrder(source.getCostCenterControllingOrder());
        clone.setContractTemplateTag(source.getContractTemplateTag());
        clone.setPriceInWords(source.getPriceInWords());
        clone.setPriceFormula(source.getPriceFormula());
        clone.setIssuedSeparateInvoice(source.getIssuedSeparateInvoice());
        clone.setConditions(source.getConditions());
        clone.setStatus(PriceComponentStatus.ACTIVE);
        clone.setPriceComponentGroupDetailId(null); // clone is "available"
        PriceComponent clonedPriceComponent = priceComponentRepository.saveAndFlush(clone);

        clonePriceComponentFormulaVariables(source.getFormulaVariables(), clonedPriceComponent);

        // application model is mandatory when creating a price component, so it will always be present
        ApplicationModel sourceApplicationModel = applicationModelRepository
                .findByPriceComponentIdAndStatusIn(source.getId(), List.of(ApplicationModelStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Application model of price component with ID %s not found;".formatted(source.getId())));

        cloneApplicationModel(sourceApplicationModel, clonedPriceComponent);

        return priceComponentRepository.saveAndFlush(clone);
    }

    public List<ApplicationModel> copyPriceComponentsWithResponse(List<ApplicationModel> applicationModelsToCopy) {
        List<ApplicationModel> newApplicationModels = new ArrayList<>();
        for (ApplicationModel item : applicationModelsToCopy) {
            ApplicationModel applicationModel = copyApplicationModelWithPriceComponent(item);
            if (applicationModel != null) {
                newApplicationModels.add(applicationModel);
            }
        }
        return newApplicationModels;
    }

    public ApplicationModel copyApplicationModelWithPriceComponent(ApplicationModel source) {
        ApplicationModel copied = new ApplicationModel();
        copied.setApplicationModelType(source.getApplicationModelType());
        copied.setApplicationType(source.getApplicationType());
        copied.setApplicationLevel(source.getApplicationLevel());
        copied.setStatus(source.getStatus());
        PriceComponent priceComponent = copyPriceComponent(source.getPriceComponent());
        if (priceComponent == null) {
            return null;
        }

        boolean isSuccessful = applicationModelService.copy(source, copied, priceComponent);
        if (!isSuccessful) {
            return null;
        }
        return copied;
    }

    public PriceComponent copyPriceComponent(PriceComponent source) {
        Currency currency = null;
        VatRate vatRate = null;
        PriceComponentPriceType priceType = null;
        PriceComponentValueType valueType = null;
        if (checkCurrency(source.getCurrency())) {
            currency = source.getCurrency();
        }
        if (checkVatRate(source.getVatRate(), source.getGlobalVatRate())) {
            vatRate = source.getVatRate();
        }
        if (checkPriceComponentPriceType(source.getPriceType())) {
            priceType = source.getPriceType();
        }
        if (checkPriceComponentValueType(source.getValueType())) {
            valueType = source.getValueType();
        }
        PriceComponent copied = priceComponentGroupMapper.copyPriceComponent(source);
        copied.setCurrency(currency);
        copied.setVatRate(vatRate);
        copied.setPriceType(priceType);
        copied.setValueType(valueType);
        if (currency == null || (vatRate == null && !source.getGlobalVatRate()) || priceType == null || valueType == null) {
            return null;
        }

        List<PriceComponentFormulaVariable> priceComponentFormulaVariables =
                priceComponentGroupMapper.copyFormulaVariables(source.getFormulaVariables(), copied);
        copied.setFormulaVariables(priceComponentFormulaVariables);
        return copied;
    }

    private boolean checkPriceComponentValueType(PriceComponentValueType valueType) {
        if (valueType != null) {
            return valueType.getStatus().equals(NomenclatureItemStatus.ACTIVE);
        } else return false;
    }

    private boolean checkPriceComponentPriceType(PriceComponentPriceType priceType) {
        if (priceType != null) {
            return priceType.getStatus().equals(NomenclatureItemStatus.ACTIVE);
        } else return false;
    }

    private boolean checkVatRate(VatRate vatRate, Boolean globalVatRate) {
        if (vatRate == null) {
            return false;
        }
        if (BooleanUtils.isTrue(globalVatRate)) {
            return true;
        } else {
            return vatRate.getStatus().equals(NomenclatureItemStatus.ACTIVE);
        }
    }

    private boolean checkCurrency(Currency currency) {
        if (currency != null) {
            return currency.getStatus().equals(NomenclatureItemStatus.ACTIVE);
        } else return false;
    }


    /**
     * Clones a list of price component formula variables and saves them to the database.
     * The provided price component is the owner of the variables. This is one step of price component cloning process,
     * cloning price component and application models should be performed separately.
     *
     * @param sources        the list of variables to be cloned
     * @param priceComponent the owner of the variables
     */
    private void clonePriceComponentFormulaVariables(List<PriceComponentFormulaVariable> sources, PriceComponent priceComponent) {
        for (PriceComponentFormulaVariable source : sources) {
            PriceComponentFormulaVariable target = new PriceComponentFormulaVariable();
            target.setDescription(source.getDescription());
            target.setValue(source.getValue());
            target.setValueFrom(source.getValueFrom());
            target.setValueTo(source.getValueTo());
            target.setVariable(source.getVariable());
            target.setPriceComponent(priceComponent);
            target.setProfileForBalancing(source.getProfileForBalancing());
            priceComponentFormulaVariableRepository.save(target);
        }
    }


    /**
     * Clones an application model and saves it to the database. The operation is delegated to the application model service.
     * All models and their sub entities are created by ACTIVE statuses.
     * The provided price component is the owner of the application model. This is one step of price component cloning process,
     * cloning price component and formula variables should be performed separately.
     *
     * @param source         the application model to be cloned
     * @param priceComponent the owner of the application model
     */
    private void cloneApplicationModel(ApplicationModel source, PriceComponent priceComponent) {
        applicationModelService.clone(source, priceComponent);
    }

    public List<PriceComponent> copyPriceComponentsForSubObjects(List<PriceComponent> list) {
        List<Long> ids = new ArrayList<>();
        for (PriceComponent priceComponent : list) {
            ids.add(priceComponent.getId());
        }
        List<ApplicationModel> applicationModelsToCopy = applicationModelRepository.findAllByPriceComponentIdsAndStatus(ids, List.of(ApplicationModelStatus.ACTIVE));

        return copyPriceComponentsWithResponse(applicationModelsToCopy).stream().map(ApplicationModel::getPriceComponent).toList();
    }

    public Page<ProfileForBalancingShortResponse> getBalancingProfileNames(BalancingNamesRequest request) {
        return profileForBalancingRepository
                .findAllActiveBalancingProfiles(EPBStringUtils.fromPromptToQueryParameter(request.prompt()), PageRequest.of(request.page(), request.size()))
                .map(ProfileForBalancingShortResponse::new);
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.PRICE_COMPONENT, List.of(PermissionEnum.PRICE_COMPONENT_EDIT_LOCKED));
    }

    public Page<PriceComponentTagResponse> tagFilter(PriceComponentTagFilterRequest request) {
        log.debug("Requested list for Price component tags with request: {}", request);
        Sort.Order order = new Sort.Order(checkColumnDirection(request), "contractTemplateTag");
        String columnName = "";
        if (StringUtils.isEmpty(request.getPrompt())) {
            columnName = null;
        }
        Page<String> distinctTags = priceComponentRepository.filterTags(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                columnName,
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))
        );
        return distinctTags.map(tag -> PriceComponentTagResponse
                .builder()
                .id(tag)
                .name(tag)
                .build());
    }

    private Sort.Direction checkColumnDirection(PriceComponentTagFilterRequest request) {
        if (request.getColumnDirection() == null) {
            return Sort.Direction.ASC;
        } else return request.getColumnDirection();
    }

    private List<CcyRestrictionResponse> getCopyOfCcyRestrictions(List<CcyRestrictionResponse> ccyRestriction) {
        return ccyRestriction.stream().map(intervalInfo -> CcyRestrictionResponse.builder()
                .id(intervalInfo.getId())
                .valueFrom(intervalInfo.getValueFrom())
                .valueTo(intervalInfo.getValueTo())
                .currencyResponse(intervalInfo.getCurrencyResponse().getStatus().equals(ACTIVE) ? intervalInfo.getCurrencyResponse() : null).build()).toList();
    }

}
