package bg.energo.phoenix.service.receivable.reminder;

import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionVariableName;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionVariableValue;
import bg.energo.phoenix.model.request.product.price.RuleRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ConditionInfoShortResponse;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceTypeRepository;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.RuleEvaluatorService;
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
public class ReminderConditionService {

    private final ExpressionStringParser expressionStringParser;
    private final RuleEvaluatorService ruleEvaluatorService;

    private final GridOperatorRepository gridOperatorRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SegmentRepository segmentRepository;

    public void validateCondition(String conditions, List<String> errorMessages) {
        if (validateConditionLogicalOperatorType(conditions, errorMessages) && validateConditionNotOperatorType(conditions, errorMessages)) {
            validateCondition(
                    expressionStringParser.replaceParams(conditions),
                    getConditionVariables(conditions),
                    errorMessages
            );
        }
    }

    public boolean validateConditionNotOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.stream(ReminderConditionVariableName.values()).map(ReminderConditionVariableName::name).toList();

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
                    .stream(ReminderConditionVariableName.values())
                    .filter(pcvn -> variable.contains(pcvn.name()))
                    .findAny();

            if (conditionVariableName.isPresent()) {
                switch (conditionVariableName.get().getType()) {
                    case NUMBER -> result.put("$%s$".formatted(variable), "%s".formatted("811"));
                    case STRING -> result.put("$%s$".formatted(variable), "\"%s\"".formatted("PRIVATE_CUSTOMER"));
                }
            } else {
                var conditionVariableType = Arrays
                        .stream(ReminderConditionVariableValue.values())
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
            log.error("Error while reminder validate condition", e);
            errorMessages.add("conditions-[conditions] Error while reminder validate conditions;");
        }
    }

    private boolean validateConditionLogicalOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.asList(
                ReminderConditionVariableName.CONTRACT.name(),
                ReminderConditionVariableName.PRODUCT.name(),
                ReminderConditionVariableName.SERVICE.name(),
                ReminderConditionVariableName.PRODUCT_TYPE.name(),
                ReminderConditionVariableName.SERVICE_TYPE.name(),
                ReminderConditionVariableName.CUSTOMER_TYPE.name(),
                ReminderConditionVariableName.POD_GRID_OP.name(),
                ReminderConditionVariableName.POD_TYPE.name(),
                ReminderConditionVariableName.CONTRACT_TYPE.name(),
                ReminderConditionVariableName.POD_MEASUREMENT_TYPE.name(),
                ReminderConditionVariableName.POD_VOLTAGE_LEVEL.name(),
                ReminderConditionVariableName.PURPOSE_OF_CONSUMPTION.name(),
                ReminderConditionVariableName.INTERIM_ADVANCE_PAYMENT.name(),
                ReminderConditionVariableName.SEGMENT.name()
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
            errorMessages.add("conditions-[conditions] Illegal logical operator with variable %s;".formatted(key));
        }
    }

    public List<ConditionInfoShortResponse> getConditionsInfo(String condition) {
        if (StringUtils.isEmpty(condition)) {
            return Collections.emptyList();
        }

        List<ConditionInfoShortResponse> conditionsInfo = new ArrayList<>();
        Map<String, Set<Long>> parsedConditionValuesMap = parseConditionPreview(condition);

        Map<String, Function<List<Long>, List<ActivityNomenclatureResponse>>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(ReminderConditionVariableName.PRODUCT_TYPE.name(), productTypeRepository::findByIdIn);
        repositoryLookups.put(ReminderConditionVariableName.SERVICE_TYPE.name(), serviceTypeRepository::findByIdIn);
        repositoryLookups.put(ReminderConditionVariableName.POD_GRID_OP.name(), gridOperatorRepository::findByIdIn);
        repositoryLookups.put(ReminderConditionVariableName.SEGMENT.name(), segmentRepository::findByIdIn);

        for (Map.Entry<String, Set<Long>> entry : parsedConditionValuesMap.entrySet()) {
            String key = entry.getKey();
            Set<Long> values = entry.getValue();

            if (repositoryLookups.containsKey(key)) {
                List<ActivityNomenclatureResponse> entities = repositoryLookups.get(key).apply(values.stream().toList());
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
                ReminderConditionVariableName.PRODUCT_TYPE.name(),
                ReminderConditionVariableName.SERVICE_TYPE.name(),
                ReminderConditionVariableName.POD_GRID_OP.name(),
                ReminderConditionVariableName.SEGMENT.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        parseConditionType(condition, regex + "in\\s*\\(([^)]+)\\)", resultMap); // "in" conditions
        parseConditionType(condition, regex + "=\\s*(\\$?\\d+\\$?)", resultMap); // "=" conditions
        parseConditionType(condition, regex + "<>\\s*(\\$?\\d+\\$?)", resultMap); // "<>" conditions

        return resultMap;
    }

    private static void parseConditionType(String condition,
                                           String regex,
                                           Map<String, Set<Long>> resultMap) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);

        while (matcher.find()) {
            String key = matcher.group(1);
            Set<Long> values = resultMap.getOrDefault(key, new HashSet<>());
            String[] inValues = matcher.group(2).split(",\\s*");
            values.addAll(Arrays.stream(inValues).map(v -> Long.valueOf(v.replace("$", ""))).toList());
            resultMap.put(key, values);
        }
    }

}
