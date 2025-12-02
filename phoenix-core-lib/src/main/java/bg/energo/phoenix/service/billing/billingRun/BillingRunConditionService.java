package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.BillingRunConditionVariableName;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunConditionVariableValue;
import bg.energo.phoenix.model.request.product.price.RuleRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionPreviewInfo;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.pod.PodAdditionalParametersRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceTypeRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
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
public class BillingRunConditionService {
    private final RuleEvaluatorService ruleEvaluatorService;
    private final ExpressionStringParser expressionStringParser;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final SegmentRepository segmentRepository;
    private final PodAdditionalParametersRepository podAdditionalParametersRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CustomerRepository customerRepository;

    public void validateBillingRunCondition(String condition, List<String> errorMessages, String errorText) {
        if (validateConditionLogicalOperatorType(condition, errorMessages, errorText)
                && validateConditionNotOperatorType(condition, errorMessages, errorText)) {
            validateCondition(
                    expressionStringParser.replaceParams(condition),
                    getConditionVariables(condition),
                    errorMessages
            );
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
    }

    private void validateCondition(String condition, Map<String, Object> variables, List<String> errorMessages) {
        try {
            RuleRequest ruleRequest = new RuleRequest();
            ruleRequest.setExpression(condition);
            ruleRequest.setArguments(variables);
            ruleEvaluatorService.evaluateBooleanExpression(ruleRequest);
        } catch (Exception e) {
            log.error("Error while validate condition ", e);
            errorMessages.add("condition-condition is not valid;");
        }
    }

    private Map<String, Object> getConditionVariables(String condition) {
        if (StringUtils.isBlank(condition)) {
            return new HashMap<>();
        }
        var variables = expressionStringParser.getVariables(condition);
        Map<String, Object> result = new HashMap<>();
        for (String variable : variables) {
            var conditionVariableName = Arrays.stream(BillingRunConditionVariableName.values())
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
                var conditionVariableType = Arrays.stream(BillingRunConditionVariableValue.values())
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

    public boolean validateConditionLogicalOperatorType(String condition, List<String> errorMessages, String errorText) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.asList(
                BillingRunConditionVariableName.CONTRACT.name(),
                BillingRunConditionVariableName.PRODUCT.name(),
                BillingRunConditionVariableName.SERVICE.name(),
                BillingRunConditionVariableName.PRODUCT_TYPE.name(),
                BillingRunConditionVariableName.SERVICE_TYPE.name(),
                BillingRunConditionVariableName.CUSTOMER_TYPE.name(),
                BillingRunConditionVariableName.GRID_OP.name(),
                BillingRunConditionVariableName.CONTRACT_TYPE.name(),
                BillingRunConditionVariableName.MEASUREMENT_TYPE.name(),
                BillingRunConditionVariableName.VOLTAGE_LEVEL.name(),
                BillingRunConditionVariableName.PURPOSE_OF_CONSUMPTION.name(),
                BillingRunConditionVariableName.INTERIM_ADVANCE_PAYMENT.name(),
                BillingRunConditionVariableName.CUSTOMER_SEGMENT.name(),
                BillingRunConditionVariableName.POD_ADDITIONAL_PARAMETER.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, regex + ">\\s*(\\$?\\.*+\\$?)", result, errorMessages, errorText); // ">" conditions
        validateLogicalOperator(condition, regex + "<(\\$\\.*+\\$?)", result, errorMessages, errorText); // "<" conditions

        return result.isEmpty();
    }

    public boolean validateConditionNotOperatorType(String condition, List<String> errorMessages, String errorText) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.stream(BillingRunConditionVariableName.values()).map(BillingRunConditionVariableName::name).toList();

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, "NOT" + regex, result, errorMessages, "%s Illegal syntax of NOT operator with variable".formatted(errorText)); // "NOT" conditions

        return result.isEmpty();
    }

    private static void validateLogicalOperator(String condition,
                                                String regex,
                                                List<String> keys,
                                                List<String> errorMessages,
                                                String errorText) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);

        while (matcher.find()) {
            String key = matcher.group(1);
            keys.add(key);
            errorMessages.add("%s %s;".formatted(errorText, key));
        }
    }

    public List<ConditionPreviewInfo> getConditionsInfo(String condition) {
        if (StringUtils.isEmpty(condition)) {
            return Collections.emptyList();
        }

        List<ConditionPreviewInfo> conditionsInfo = new ArrayList<>();
        Map<String, Set<Long>> parsedConditionValuesMap = parseCondition(condition);

        Map<String, Function<List<Long>, List<ActivityNomenclatureResponse>>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(BillingRunConditionVariableName.PRODUCT_TYPE.toString(), productTypeRepository::findByIdIn);
        repositoryLookups.put(BillingRunConditionVariableName.SERVICE_TYPE.toString(), serviceTypeRepository::findByIdIn);
        repositoryLookups.put(BillingRunConditionVariableName.GRID_OP.toString(), gridOperatorRepository::findByIdIn);
        repositoryLookups.put(BillingRunConditionVariableName.CUSTOMER_SEGMENT.toString(), segmentRepository::findByIdIn);
        repositoryLookups.put(BillingRunConditionVariableName.POD_ADDITIONAL_PARAMETER.toString(), podAdditionalParametersRepository::findByIdIn);

        Map<String, Function<List<Long>, List<ConditionParameterResponse>>> objectRepositoryLookups = new HashMap<>();
        objectRepositoryLookups.put(BillingRunConditionVariableName.PRODUCT.name(), productRepository::findByIdIn);
        objectRepositoryLookups.put(BillingRunConditionVariableName.SERVICE.toString(), serviceRepository::findByIdIn);
        objectRepositoryLookups.put(BillingRunConditionVariableName.CUSTOMER_NUMBER.toString(), customerRepository::findByIdIn);

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

    private static Map<String, Set<Long>> parseCondition(String condition) {
        Map<String, Set<Long>> resultMap = new HashMap<>();

        // Every new nomenclature (if added to types) should also be added to the regex
        List<String> nomenclatureNames = Arrays.asList(
                BillingRunConditionVariableName.PRODUCT.name(),
                BillingRunConditionVariableName.SERVICE.name(),
                BillingRunConditionVariableName.PRODUCT_TYPE.name(),
                BillingRunConditionVariableName.SERVICE_TYPE.name(),
                BillingRunConditionVariableName.GRID_OP.name(),
                BillingRunConditionVariableName.CUSTOMER_SEGMENT.name(),
                BillingRunConditionVariableName.POD_ADDITIONAL_PARAMETER.name(),
                BillingRunConditionVariableName.CUSTOMER_NUMBER.name()
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

    private void addConditionInfoToList(Map.Entry<String, Set<Long>> entry,
                                        String valueName,
                                        Long valueId,
                                        List<ConditionPreviewInfo> conditionsInfo) {
        ConditionPreviewInfo conditionPreviewInfo = new ConditionPreviewInfo();
        conditionPreviewInfo.setKey(entry.getKey());
        conditionPreviewInfo.setName(valueName);
        conditionPreviewInfo.setId(valueId);
        conditionsInfo.add(conditionPreviewInfo);
    }
}
