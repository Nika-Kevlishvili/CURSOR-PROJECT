package bg.energo.phoenix.service.receivable.massOperationForBlocking;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlocking;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelConditionVariableName;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingConditionVariableName;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingConditionVariableValue;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingType;
import bg.energo.phoenix.model.request.product.price.RuleRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ConditionEvaluationResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ConditionInfoShortResponse;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceTypeRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingRepository;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.RuleEvaluatorService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableBlockingConditionService {

    private final ExpressionStringParser expressionStringParser;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;

    private final GridOperatorRepository gridOperatorRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SegmentRepository segmentRepository;
    private final ReceivableBlockingRepository receivableBlockingRepository;


    public void validateCondition(String conditions, List<String> errorMessages) {
        if (validateConditionLogicalOperatorType(conditions, errorMessages) && validateConditionNotOperatorType(conditions, errorMessages)) {
            validateCondition(
                    expressionStringParser.replaceParams(conditions),
                    getConditionVariables(conditions),
                    errorMessages
            );
        }
    }

    /**
     * Evaluates the condition specified in the given receivable blocking and returns the IDs of the receivables, liabilities, and payments that match the condition.
     *
     * @param blockingId the ID of the receivable blocking to evaluate
     * @return a ConditionEvaluationResponse containing the IDs of the matching receivables, liabilities, and payments
     * @throws DomainEntityNotFoundException if the receivable blocking with the given ID cannot be found
     * @throws IllegalArgumentException if the receivable blocking does not have a condition set
     */
    public ConditionEvaluationResponse conditionResult(Long blockingId) {
        log.info("get condition result from mass operation blocking object with id: %s;".formatted(blockingId));
        List<String> errorMessages = new ArrayList<>();
        ReceivableBlocking receivableBlocking = receivableBlockingRepository.findById(blockingId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find Mass operation for blocking with id: %s;".formatted(blockingId))
                );

        if (receivableBlocking.getCustomerConditions() == null) {
            throw new IllegalArgumentException("Condition isn't set on this object !;");
        }

        String condition = receivableBlocking.getCustomerConditions();
        validateCondition(condition, errorMessages);
        validateConditionKeys(condition, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        ConditionEvaluationResponse conditionEvaluationResponse = new ConditionEvaluationResponse();
        for (ReceivableBlockingType type : receivableBlocking.getBlockingTypes()) {
            if (type.equals(ReceivableBlockingType.CUSTOMER_RECEIVABLE)) {
                List<Long> receivableIds = receivableBlockingRepository.getReceivableByCondition(condition);
                conditionEvaluationResponse.setReceivableIds(receivableIds);
            }
            if (type.equals(ReceivableBlockingType.CUSTOMER_LIABILITY)) {
                List<Long> liabilitiesIds = receivableBlockingRepository.getLiabilitiesByCondition(condition);
                conditionEvaluationResponse.setLiabilitiesIds(liabilitiesIds);
            }
            if (type.equals(ReceivableBlockingType.PAYMENT)) {
                List<Long> paymentsIds = receivableBlockingRepository.getPaymentsByCondition(condition);
                conditionEvaluationResponse.setPaymentIds(paymentsIds);
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return conditionEvaluationResponse;
    }

    /**
     * Validates the condition keys in the provided condition string.
     *
     * This method checks that all the keys in the condition string are present in the list of valid condition keys
     * retrieved from the `receivableBlockingRepository`. If any key is not found, an `IllegalArgumentException`
     * is thrown with a message indicating the invalid key.
     *
     * @param condition The condition string to validate.
     * @throws IllegalArgumentException if any of the keys in the condition string are not valid.
     */
    public void validateConditionKeys(String condition, List<String> errorMessages) {
        List<String> conditionKeys = receivableBlockingRepository.getConditionKeys();
        Set<String> parsedKeys = keyParse(condition);

        for (String key : parsedKeys) {
            if (!conditionKeys.contains(key)) {
                errorMessages.add("Invalid condition key: " + key);
            }
        }
    }


    /**
     * Parses the given input string and returns a set of keys found in the input.
     * The keys are extracted using a regular expression that matches the pattern `$<word>$` where `<word>` does not contain the words "AND", "OR", or "NOT".
     * The extracted keys are returned as a set to avoid duplicates.
     *
     * @param input the input string to parse
     * @return a set of keys found in the input
     */
    public Set<String> keyParse(String input) {
        Set<String> keys = new HashSet<>();

        String regex = "\\$((?!AND|OR|NOT)\\w+)\\$\\s*(?:=|<>|<|>|IN)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            keys.add("$" + matcher.group(1) + "$");
        }

        return keys;
    }


    public boolean validateConditionNotOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.stream(ReceivableBlockingConditionVariableName.values()).map(ReceivableBlockingConditionVariableName::name).toList();

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, "NOT" + regex, result, errorMessages); // "NOT" condition

        return result.isEmpty();
    }


    private Map<String, Object> getConditionVariables(String condition) {
        if (StringUtils.isBlank(condition)) {
            return new HashMap<>();
        }
        var variables = expressionStringParser.getVariables(condition);
        Map<String, Object> result = new HashMap<>();
        for (String variable : variables) {
            var conditionVariableName = Arrays
                    .stream(ReceivableBlockingConditionVariableName.values())
                    .filter(pcvn -> variable.contains(pcvn.name()))
                    .findAny();

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
                var conditionVariableType = Arrays
                        .stream(ReceivableBlockingConditionVariableValue.values())
                        .filter(pcvt -> variable.contains(pcvt.name()))
                        .findAny();
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
            log.error("Error while receivable blocking validate condition", e);
            errorMessages.add("condition- [condition] Error while receivable blocking validate condition;");
        }
    }

    private boolean validateConditionLogicalOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.asList(
                ReceivableBlockingConditionVariableName.CONTRACT.name(),
                ReceivableBlockingConditionVariableName.PRODUCT.name(),
                ReceivableBlockingConditionVariableName.SERVICE.name(),
                ReceivableBlockingConditionVariableName.PRODUCT_TYPE.name(),
                ReceivableBlockingConditionVariableName.SERVICE_TYPE.name(),
                ReceivableBlockingConditionVariableName.CUSTOMER_TYPE.name(),
                ReceivableBlockingConditionVariableName.POD_GRID_OP.name(),
                ReceivableBlockingConditionVariableName.POD_TYPE.name(),
                ReceivableBlockingConditionVariableName.CONTRACT_TYPE.name(),
                ReceivableBlockingConditionVariableName.POD_MEASUREMENT_TYPE.name(),
                ReceivableBlockingConditionVariableName.POD_VOLTAGE_LEVEL.name(),
                ReceivableBlockingConditionVariableName.PURPOSE_OF_CONSUMPTION.name(),
                ReceivableBlockingConditionVariableName.INTERIM_ADVANCE_PAYMENT.name(),
                ReceivableBlockingConditionVariableName.SEGMENT.name(),
                ReceivableBlockingConditionVariableName.COLLECTION_CHANNEL.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, regex + ">\\s*(\\$?\\.*+\\$?)", result, errorMessages); // ">" conditions
        validateLogicalOperator(condition, regex + "<(\\$\\.*+\\$?)", result, errorMessages); // "<" conditions

        return result.isEmpty();
    }

    private static void validateLogicalOperator(String condition,
                                                String regex,
                                                List<String> keys,
                                                List<String> errorMessages
    ) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);

        while (matcher.find()) {
            String key = matcher.group(1);
            keys.add(key);
            errorMessages.add("condition- [condition] Illegal logical operator with variable %s;".formatted(key));
        }
    }

    public List<ConditionInfoShortResponse> getConditionsInfo(String condition) {
        if (StringUtils.isEmpty(condition)) {
            return Collections.emptyList();
        }

        List<ConditionInfoShortResponse> conditionsInfo = new ArrayList<>();
        Map<String, Set<Long>> parsedConditionValuesMap = parseConditionPreview(condition);

        Map<String, Function<List<Long>, List<ActivityNomenclatureResponse>>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(ReceivableBlockingConditionVariableName.PRODUCT_TYPE.name(), productTypeRepository::findByIdIn);
        repositoryLookups.put(ReceivableBlockingConditionVariableName.SERVICE_TYPE.name(), serviceTypeRepository::findByIdIn);
        repositoryLookups.put(ReceivableBlockingConditionVariableName.POD_GRID_OP.name(), gridOperatorRepository::findByIdIn);
        repositoryLookups.put(ReceivableBlockingConditionVariableName.SEGMENT.name(), segmentRepository::findByIdIn);

        Map<String, Function<List<Long>, List<ConditionParameterResponse>>> objectRepositoryLookups = new HashMap<>();
        objectRepositoryLookups.put(CollectionChannelConditionVariableName.PRODUCT.name(), productRepository::findByIdIn);
        objectRepositoryLookups.put(CollectionChannelConditionVariableName.SERVICE.name(), serviceRepository::findByIdIn);

        for (Map.Entry<String, Set<Long>> entry : parsedConditionValuesMap.entrySet()) {
            String key = entry.getKey();
            Set<Long> values = entry.getValue();

            if (repositoryLookups.containsKey(key)) {
                List<ActivityNomenclatureResponse> entities = repositoryLookups.get(key).apply(values.stream().toList());
                entities.forEach(entity -> addConditionInfoToList(entry, entity.getName(), entity.getId(), conditionsInfo));
            }
            if (objectRepositoryLookups.containsKey(key)) {
                List<ConditionParameterResponse> entities = objectRepositoryLookups.get(key).apply(values.stream().toList());
                entities.forEach(entity -> addConditionInfoToList(entry, entity.getName(), entity.getId(), conditionsInfo));
            }
        }

        return conditionsInfo;
    }

    private void addConditionInfoToList(Map.Entry<String, Set<Long>> entry,
                                        String valueName,
                                        Long valueId,
                                        List<ConditionInfoShortResponse> conditionsInfo
    ) {
        conditionsInfo.add(
                new ConditionInfoShortResponse(
                        valueId,
                        valueName,
                        entry.getKey()
                )
        );
    }


    private static Map<String, Set<Long>> parseConditionPreview(String condition) {
        Map<String, Set<Long>> resultMap = new HashMap<>();

        // Every new nomenclature (if added to types) should also be added to the regex
        List<String> nomenclatureNames = Arrays.asList(
                CollectionChannelConditionVariableName.PRODUCT.name(),
                CollectionChannelConditionVariableName.SERVICE.name(),
                CollectionChannelConditionVariableName.PRODUCT_TYPE.name(),
                CollectionChannelConditionVariableName.SERVICE_TYPE.name(),
                CollectionChannelConditionVariableName.POD_GRID_OP.name(),
                CollectionChannelConditionVariableName.CUSTOMER_SEGMENT.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*(IN\\s*\\(([^)]+)\\)|(=|<>)\\s*(\\$?\\d+\\$?))";
        parseConditionType(condition, regex, resultMap);

        return resultMap;
    }

    private static void parseConditionType(String condition,
                                           String regex,
                                           Map<String, Set<Long>> resultMap
    ) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);

        while (matcher.find()) {
            String key = matcher.group(1);
            String values = matcher.group(3);
            String value = matcher.group(5);

            Set<Long> valueSet = resultMap.getOrDefault(key, new HashSet<>());

            if (values != null) {
                for (String v : values.split(",\\s*")) {
                    valueSet.add(Long.parseLong(v.replace("$", "")));
                }
            } else if (value != null) {
                valueSet.add(Long.parseLong(value.replace("$", "")));
            }

            resultMap.put(key, valueSet);
        }
    }

}
