package bg.energo.phoenix.service.receivable.collectionChannel;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelConditionVariableName;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelConditionVariableValue;
import bg.energo.phoenix.model.request.product.price.RuleRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse;
import bg.energo.phoenix.model.response.receivable.collectionChannel.ConditionInfo;
import bg.energo.phoenix.model.response.receivable.collectionChannel.ConditionValidateResponse;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceTypeRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.RuleEvaluatorService;
import bg.energo.phoenix.service.receivable.massOperationForBlocking.ReceivableBlockingConditionService;
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
public class CollectionChannelConditionService {

    private final RuleEvaluatorService ruleEvaluatorService;
    private final ExpressionStringParser expressionStringParser;
    private final ProductTypeRepository productTypeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final GridOperatorRepository gridOperatorRepository;

    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final SegmentRepository segmentRepository;
    private final ReceivableBlockingConditionService receivableBlockingConditionService;
    private final CollectionChannelRepository collectionChannelRepository;

    private static void validateLogicalOperator(String condition,
                                                String regex,
                                                List<String> keys,
                                                List<String> errorMessages) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);

        while (matcher.find()) {
            String key = matcher.group(1);
            keys.add(key);
            errorMessages.add("condition- [condition] Illegal logical operator with variable %s;".formatted(key));
        }
    }

    private static Map<String, Set<Long>> parseCondition(String condition) {
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

    private static void parseConditionType(String condition, String regex, Map<String, Set<Long>> resultMap) {
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

    /**
     * Retrieves the result of a condition associated with a collection channel.
     *
     * @param collectionChannelId the ID of the collection channel to retrieve the condition result for
     * @return a list of liability IDs that match the condition
     * @throws DomainEntityNotFoundException if the collection channel with the given ID is not found
     * @throws IllegalArgumentException      if the collection channel does not have a condition set
     */
    public List<Long> conditionResult(Long collectionChannelId) {
        log.info("get condition result from collection channel object with id: %s;".formatted(collectionChannelId));
        List<String> errorMessages = new ArrayList<>();
        CollectionChannel collectionChannel = collectionChannelRepository.findById(collectionChannelId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find Collection channel with id: %s;".formatted(collectionChannelId))
                );

        if (collectionChannel.getCondition() == null) {
            throw new IllegalArgumentException("Condition isn't set on this object !;");
        }

        validateCondition(collectionChannel.getCondition(), errorMessages);
        validateConditionKeys(collectionChannel.getCondition(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return collectionChannelRepository.getLiabilitiesByCondition(collectionChannel.getCondition());
    }

    /**
     * Validates the condition keys in the provided condition string against the available condition keys.
     *
     * @param condition     The condition string to validate.
     * @param errorMessages A list to store any error messages encountered during validation.
     */
    public void validateConditionKeys(String condition, List<String> errorMessages) {
        List<String> conditionKeys = collectionChannelRepository.getConditionKeys();
        Set<String> parsedKeys = keyParse(condition);

        for (String key : parsedKeys) {
            if (!conditionKeys.contains(key)) {
                errorMessages.add("Invalid condition key: " + key);
            }
        }
    }

    /**
     * Parses the input string and returns a set of keys found in the string.
     * The keys are extracted using a regular expression that matches the pattern "$[a-zA-Z]+$" and excludes the keywords "AND", "OR", and "NOT".
     *
     * @param input the input string to parse
     * @return a set of keys found in the input string
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

    public void validateCondition(String conditions, List<String> errorMessages) {
        if (validateConditionLogicalOperatorType(conditions, errorMessages)) {
            validateCondition(
                    expressionStringParser.replaceParams(conditions),
                    getConditionVariables(conditions),
                    errorMessages
            );
        }
    }

    public ConditionValidateResponse validateCondition(String conditions) {
        List<String> errorMessages = new ArrayList<>();
        if (validateConditionLogicalOperatorType(conditions, errorMessages)
                && validateConditionNotOperatorType(conditions, errorMessages)) {
            validateCondition(
                    expressionStringParser.replaceParams(conditions),
                    getConditionVariables(conditions),
                    errorMessages
            );
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        }
        return new ConditionValidateResponse(true);
    }

    public boolean validateConditionLogicalOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.asList(
                CollectionChannelConditionVariableName.CONTRACT.name(),
                CollectionChannelConditionVariableName.PRODUCT.name(),
                CollectionChannelConditionVariableName.SERVICE.name(),
                CollectionChannelConditionVariableName.PRODUCT_TYPE.name(),
                CollectionChannelConditionVariableName.CUSTOMER_TYPE.name(),
                CollectionChannelConditionVariableName.POD_GRID_OP.name(),
                CollectionChannelConditionVariableName.CONTRACT_TYPE.name(),
                CollectionChannelConditionVariableName.POD_MEASUREMENT_TYPE.name(),
                CollectionChannelConditionVariableName.POD_VOLTAGE_LEVEL.name(),
                CollectionChannelConditionVariableName.PURPOSE_OF_CONSUMPTION.name(),
                CollectionChannelConditionVariableName.POD_TYPE.name(),
                CollectionChannelConditionVariableName.INTERIM_ADVANCE_PAYMENT.name(),
                CollectionChannelConditionVariableName.CUSTOMER_SEGMENT.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, regex + ">\\s*(\\$?\\.*+\\$?)", result, errorMessages); // ">" conditions
        validateLogicalOperator(condition, regex + "<(\\$\\.*+\\$?)", result, errorMessages); // "<" conditions

        return result.isEmpty();
    }

    public void validateCondition(String condition, Map<String, Object> variables, List<String> errorMessages) {
        try {
            RuleRequest ruleRequest = new RuleRequest();
            ruleRequest.setExpression(condition);
            ruleRequest.setArguments(variables);
            ruleEvaluatorService.evaluateBooleanExpression(ruleRequest);
        } catch (Exception e) {
            log.error("Error while editing Collection Channel Formula validateCondition ", e);
            errorMessages.add("condition- [condition] Error while editing Collection Channel Formula validateCondition;");
        }
    }

    public Map<String, Object> getConditionVariables(String condition) {
        if (StringUtils.isBlank(condition)) {
            return new HashMap<>();
        }
        var variables = expressionStringParser.getVariables(condition);
        Map<String, Object> result = new HashMap<>();
        for (String variable : variables) {
            var conditionVariableName = Arrays.stream(CollectionChannelConditionVariableName.values())
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
                var conditionVariableType = Arrays.stream(CollectionChannelConditionVariableValue.values())
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

    public List<ConditionInfo> getConditionInfo(String condition) {
        if (StringUtils.isEmpty(condition)) {
            return Collections.emptyList();
        }

        List<ConditionInfo> conditionsInfo = new ArrayList<>();
        Map<String, Set<Long>> parsedConditionValuesMap = parseCondition(condition);

        Map<String, Function<List<Long>, List<ActivityNomenclatureResponse>>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(CollectionChannelConditionVariableName.PRODUCT_TYPE.toString(), productTypeRepository::findByIdIn);
        repositoryLookups.put(CollectionChannelConditionVariableName.SERVICE_TYPE.toString(), serviceTypeRepository::findByIdIn);
        repositoryLookups.put(CollectionChannelConditionVariableName.POD_GRID_OP.toString(), gridOperatorRepository::findByIdIn);
        repositoryLookups.put(CollectionChannelConditionVariableName.CUSTOMER_SEGMENT.toString(), segmentRepository::findByIdIn);

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
                                        List<ConditionInfo> conditionsInfo) {
        ConditionInfo conditionInfo = new ConditionInfo();
        conditionInfo.setKey(entry.getKey());
        conditionInfo.setName(valueName);
        conditionInfo.setId(valueId);
        conditionsInfo.add(conditionInfo);
    }

    public boolean validateConditionNotOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.stream(CollectionChannelConditionVariableName.values()).map(CollectionChannelConditionVariableName::name).toList();

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, "NOT" + regex, result, errorMessages); // "NOT" condition

        return result.isEmpty();
    }
}
