package bg.energo.phoenix.service.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequests;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsConditionVariableName;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsConditionVariableValue;
import bg.energo.phoenix.model.request.product.price.RuleRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.billing.billingRun.condition.ConditionParameterResponse;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.ConditionInfoShortResponse;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.product.ProductTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceTypeRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestRepository;
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
public class DisconnectionPowerSupplyRequestsConditionService {

    private final ExpressionStringParser expressionStringParser;
    private final RuleEvaluatorService ruleEvaluatorService;

    private final GridOperatorRepository gridOperatorRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SegmentRepository segmentRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository;

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

    private static Map<String, Set<Long>> parseConditionPreview(String condition) {
        Map<String, Set<Long>> resultMap = new HashMap<>();

        // Every new nomenclature (if added to types) should also be added to the regex
        List<String> nomenclatureNames = Arrays.asList(
                DisconnectionPowerSupplyRequestsConditionVariableName.PRODUCT.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.SERVICE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.PRODUCT_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.SERVICE_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.POD_GRID_OP.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.SEGMENT.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*(IN\\s*\\(([^)]+)\\)|(=|<>)\\s*(\\$?\\d+\\$?))";
        parseConditionType(condition, regex, resultMap);

        return resultMap;
    }

    private static void parseConditionType(String condition,
                                           String regex,
                                           Map<String, Set<Long>> resultMap) {
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
     * Retrieves the condition result from the request for disconnection object with the specified ID.
     *
     * @param requestForDcnId the ID of the request for disconnection object
     * @return a list of long values representing the condition result
     * @throws DomainEntityNotFoundException if the request for disconnection object with the specified ID cannot be found
     * @throws IllegalArgumentException      if the condition is not set on the request for disconnection object
     */
    public List<Long> conditionResult(Long requestForDcnId) {
        log.info("get condition result from request for disconnection object with id: %s;".formatted(requestForDcnId));
        List<String> errorMessages = new ArrayList<>();
        DisconnectionPowerSupplyRequests disconnectionOfPowerSupply = disconnectionPowerSupplyRequestRepository.findById(requestForDcnId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find request of disconnection with id: %s".formatted(requestForDcnId))
                );

        if (disconnectionOfPowerSupply.getCondition() == null) {
            throw new IllegalArgumentException("Condition isn't set on this object !;");
        }

        validateCondition(disconnectionOfPowerSupply.getCondition(), errorMessages);
        validateConditionKeys(disconnectionOfPowerSupply.getCondition(), errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        return disconnectionPowerSupplyRequestRepository.getOverdueLiabilitiesByCondition(disconnectionOfPowerSupply.getCondition());

    }

    /**
     * Validates the condition keys in a disconnection power supply request.
     *
     * @param condition     The condition to validate.
     * @param errorMessages The list to add any error messages to.
     */
    public void validateConditionKeys(String condition, List<String> errorMessages) {
        List<String> conditionKeys = disconnectionPowerSupplyRequestRepository.getConditionKeys();
        Set<String> parsedKeys = keyParse(condition);

        for (String key : parsedKeys) {
            if (!conditionKeys.contains(key)) {
                errorMessages.add("Invalid condition key: " + key);
            }
        }
    }

    /**
     * Parses the given input string and returns a set of keys that match the specified regular expression pattern.
     * The pattern looks for strings in the format "$[a-zA-Z]+$" that are followed by an equality, inequality, or "IN" operator.
     * The returned set contains the matched keys, including the "$" prefix and suffix.
     *
     * @param input the input string to parse
     * @return a set of parsed keys
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

        List<String> nomenclatureNames = Arrays.stream(DisconnectionPowerSupplyRequestsConditionVariableName.values()).map(DisconnectionPowerSupplyRequestsConditionVariableName::name).toList();

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
                    .stream(DisconnectionPowerSupplyRequestsConditionVariableName.values())
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
                        .stream(DisconnectionPowerSupplyRequestsConditionVariableValue.values())
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
            log.error("Error while Disconnection power supply request validate condition", e);
            errorMessages.add("[condition] customer_under_conditions_is_not_valid");
        }
    }

    private boolean validateConditionLogicalOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.asList(
                DisconnectionPowerSupplyRequestsConditionVariableName.CONTRACT.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.PRODUCT.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.SERVICE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.PRODUCT_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.SERVICE_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.CUSTOMER_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.POD_GRID_OP.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.POD_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.CONTRACT_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.POD_MEASUREMENT_TYPE.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.POD_VOLTAGE_LEVEL.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.PURPOSE_OF_CONSUMPTION.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.INTERIM_ADVANCE_PAYMENT.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.SEGMENT.name(),
                DisconnectionPowerSupplyRequestsConditionVariableName.COLLECTION_CHANNEL.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, regex + ">\\s*(\\$?\\.*+\\$?)", result, errorMessages); // ">" conditions
        validateLogicalOperator(condition, regex + "<(\\$\\.*+\\$?)", result, errorMessages); // "<" conditions

        return result.isEmpty();
    }

    public List<ConditionInfoShortResponse> getConditionsInfo(String condition) {
        if (StringUtils.isEmpty(condition)) {
            return Collections.emptyList();
        }

        List<ConditionInfoShortResponse> conditionsInfo = new ArrayList<>();
        Map<String, Set<Long>> parsedConditionValuesMap = parseConditionPreview(condition);

        Map<String, Function<List<Long>, List<ActivityNomenclatureResponse>>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(DisconnectionPowerSupplyRequestsConditionVariableName.PRODUCT_TYPE.toString(), productTypeRepository::findByIdIn);
        repositoryLookups.put(DisconnectionPowerSupplyRequestsConditionVariableName.SERVICE_TYPE.toString(), serviceTypeRepository::findByIdIn);
        repositoryLookups.put(DisconnectionPowerSupplyRequestsConditionVariableName.POD_GRID_OP.toString(), gridOperatorRepository::findByIdIn);
        repositoryLookups.put(DisconnectionPowerSupplyRequestsConditionVariableName.SEGMENT.toString(), segmentRepository::findByIdIn);

        Map<String, Function<List<Long>, List<ConditionParameterResponse>>> objectRepositoryLookups = new HashMap<>();
        objectRepositoryLookups.put(DisconnectionPowerSupplyRequestsConditionVariableName.PRODUCT.name(), productRepository::findByIdIn);
        objectRepositoryLookups.put(DisconnectionPowerSupplyRequestsConditionVariableName.SERVICE.name(), serviceRepository::findByIdIn);

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

}
