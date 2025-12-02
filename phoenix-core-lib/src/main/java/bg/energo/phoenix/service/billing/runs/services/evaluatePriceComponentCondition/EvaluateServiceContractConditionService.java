package bg.energo.phoenix.service.billing.runs.services.evaluatePriceComponentCondition;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableValue;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.customer.PreferencesRepository;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.service.billing.runs.models.evaluatePriceComponentCondition.PriceComponentEvaluationPrimitiveVariableMapper;
import bg.energo.phoenix.service.billing.runs.models.evaluatePriceComponentCondition.TempPriceConditionModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableForParsing.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluateServiceContractConditionService {
    private final PriceComponentRepository priceComponentRepository;
    private final PreferencesRepository preferencesRepository;
    private final CampaignRepository campaignRepository;
    private final SegmentRepository segmentRepository;
    private final SqlCommandValidationService sqlCommandValidationService;

    @PersistenceContext
    private EntityManager em;

    public List<TempPriceConditionModel> evaluateConditionWithQuery(List<TempPriceConditionModel> billingDataInvoiceModels) {
        List<TempPriceConditionModel> finalList = new ArrayList<>();
        //group data by priceComponentId
        Map<Long, List<TempPriceConditionModel>> billingDataInvoiceModelsByPriceComponent = billingDataInvoiceModels
                .stream()
                .collect(groupingBy(TempPriceConditionModel::getPriceComponentId, toList()));
        for (Long priceComponentId : billingDataInvoiceModelsByPriceComponent.keySet()) {
            Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
            //if price Component does not exist with  priceComponentId id should ignore all data with this price component
            if (priceComponentOptional.isEmpty()) {
                continue;
            }
            String condition = priceComponentOptional.get().getConditions();
            //if there is no condition in price component should add into response
            if (StringUtils.isEmpty(condition)) {
                finalList.addAll(billingDataInvoiceModelsByPriceComponent.get(priceComponentId));
                continue;
            }
            //check on sql commands
            if (isConditionHasSqlCommands(condition)) {
                log.error("SQL commands are not allowed in the condition price component %s".formatted(priceComponentId));
                continue;
            }
            //replace all redundant conditions with true logical operator
            condition = replaceRedundantConditions(condition);
            //validate condition on valid nomenclature ids and if failed ignore all data with this price component
            if (!validateAllNomenclatureValuesInCondition(condition)) {
                log.error("There are incorrect nomenclature ids in condition");
                continue;
            }
            //generate dynamic query based on condition
            createDynamicQuery(billingDataInvoiceModelsByPriceComponent.get(priceComponentId), condition, finalList, priceComponentId);
        }

        return finalList;
    }

    private boolean isConditionHasSqlCommands(String condition) {
        String originalCondition = condition;
        return sqlCommandValidationService.containsSqlCommands(originalCondition)
                || sqlCommandValidationService.checkWithoutVariableSymbolCondition(originalCondition)
                || sqlCommandValidationService.checkWithoutSpacesCondition(originalCondition)
                || sqlCommandValidationService.checkWithoutVariableSymbolAnSpacesCondition(originalCondition);
    }

    private void createDynamicQuery(List<TempPriceConditionModel> tempPriceConditionModels, String condition, List<TempPriceConditionModel> finalList, Long priceComponentId) {
        String querySelectFrom = generateQuerySelectPart(tempPriceConditionModels);
        //append condition query for complex variables
        condition = appendConditionQueryForComplexVariables(condition);
        //remove and add necessary syntax characters into condition query
        condition = improveConditionQuerySyntax(condition);
        //append condition query for primitive variables
        condition = appendConditionQueryForPrimitiveVariables(condition);
        condition = condition.replace(" job1n ", " join ");
        condition = condition.replace("product_contract.contract_bill1ng_groups", "product_contract.contract_billing_groups");
        //after all variable name changed with table and column names, set condition into query
        String finalQuery = querySelectFrom + " AND (" + condition + ")";
        getResultFromDbWithGeneratedQuery(finalQuery, finalList, priceComponentId);
    }

    private String generateQuerySelectPart(List<TempPriceConditionModel> tempPriceConditionModels) {
        String querySelectFrom = """
                select prodcontDet.id as contractDetailId
                from service_contract.contract_details prodContDet
                         join customer.customer_details custDet
                              on prodContDet.customer_detail_id = custDet.id
                         join customer.customers cust
                              on cust.id = custDet.customer_id
                where prodContDet.id in (:productContractIdsArg)""";

        List<String> productContractIds = tempPriceConditionModels.stream().map(model -> String.valueOf(model.getContractDetailId())).toList();

        querySelectFrom = querySelectFrom.replace(":productContractIdsArg", String.join(",", productContractIds));
        return querySelectFrom;
    }

    private String improveConditionQuerySyntax(String condition) {
        condition = condition.replace("$", " ");
        condition = condition.replace("AND", " AND ");
        condition = condition.replace("OR", " OR ");
        condition = condition.replace("join", " job1n ");
        condition = condition.replace("product_contract.contract_billing_groups", "product_contract.contract_bill1ng_groups");
        condition = condition.replace("in", " in ");
        condition = condition.replace("NOT", " NOT ");
        condition = condition.replace("NON_HOUSEHOLD", "NON_H1OUSEHOLD");

        return condition;
    }

    private String appendConditionQueryForPrimitiveVariables(String condition) {
        for (PriceComponentEvaluationPrimitiveVariableMapper variableMapper : PriceComponentEvaluationPrimitiveVariableMapper.values()) {
            if (condition.contains(variableMapper.name())) {
                //column name in where for primitive types
                condition = condition.replaceAll(variableMapper.name(), variableMapper.getColumnName());
                for (PriceComponentConditionVariableValue hardcodedValue : variableMapper.getHardcodedValues()) {
                    condition = condition.replaceAll(hardcodedValue.toString(), "\\'" + hardcodedValue + "\\'");
                }
            }
        }
        condition = condition.replace("NON_H1OUSEHOLD", "\'NON_HOUSEHOLD\'");
        return condition;
    }

    private String appendConditionQueryForComplexVariables(String condition) {
        if (condition.contains("CUSTOMER_SEGMENT")) {
            condition = parseCustomerSegmentVariable(condition);
        }
        if (condition.contains("CUSTOMER_PREFERENCES")) {
            condition = parseCustomerPreferencesVariable(condition);
        }
        if (condition.contains("DIRECT_DEBIT")) {
            condition = parseDirectDebitVariable(condition);
        }
        if (condition.contains("CONTRACT_SUB_STATUS_IN_PERPETUITY")) {
            condition = parsePerpetuityVariable(condition);
        }

        return condition;
    }

    private void getResultFromDbWithGeneratedQuery(String finalQuery, List<TempPriceConditionModel> finalList, Long priceComponentId) {
        try {
            List<Object[]> result = em.createNativeQuery(finalQuery).getResultList();
            for (Object obj : result) {
                TempPriceConditionModel tempPriceConditionModel = new TempPriceConditionModel();
                tempPriceConditionModel.setContractDetailId(((BigInteger) (obj)).longValue());
                tempPriceConditionModel.setPriceComponentId(priceComponentId);
                finalList.add(tempPriceConditionModel);
            }
        } catch (Exception e) {
            log.error("Error executing query: {}", finalQuery, e);
        }
    }

    private String parseCustomerSegmentVariable(String condition) {
        String regex = "\\$?(" + "CUSTOMER_SEGMENT" + ")\\$?\\s*";
        condition = customerSegmentVariableInOperators(condition, regex + "in\\s*\\((.*?)\\)");
        condition = customerSegmentVariableEqualOperators(condition, regex + "(=\\s*\\$?\\d+\\$?)");
        condition = customerSegmentVariableNotEqualOperators(condition, regex + "<>\\s*(\\$?\\d+\\$?)");

        return condition;
    }

    private String customerSegmentVariableEqualOperators(String condition, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(2);
            String newValue = " " + """
                    (exists (select 1
                                  from (select cusSeg.segment_id segmentid
                                        from service_contract.contract_details pdcDet
                                                 join customer.customer_details cusDet on pdcDet.customer_detail_id = cusDet.id
                                                 join customer.customer_segments cusSeg on cusSeg.customer_detail_id = cusDet.id
                                        where pdcDet.id = prodContDet.id) as customerSegments
                                  where coalesce(customerSegments.segmentid, -1)
                                    """ + " " + idsPart + " " + """
                    ))""";

            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    private String customerSegmentVariableInOperators(String condition, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(2);
            String newValue = " " + """
                    (
                    (select COUNT(1) > 0
                     from (select cusSeg.segment_id as segmentid
                           from service_contract.contract_details pdcDet
                                    join customer.customer_details cusDet
                                         on pdcDet.customer_detail_id = cusDet.id
                                    join customer.customer_segments cusSeg
                                         on cusSeg.customer_detail_id = cusDet.id
                           where pdcDet.id = prodContDet.id) as customerSegments
                     where coalesce(customerSegments.segmentid, -1) in(
                                    """ + " " + idsPart + " " + """
                    )))""";

            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    private String customerSegmentVariableNotEqualOperators(String condition, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(2);
            String newValue = " " + """
                    (not exists (select 1
                                  from (select cusSeg.segment_id segmentid
                                        from service_contract.contract_details pdcDet
                                                 join customer.customer_details cusDet on pdcDet.customer_detail_id = cusDet.id
                                                 join customer.customer_segments cusSeg on cusSeg.customer_detail_id = cusDet.id
                                        where pdcDet.id = prodContDet.id) as customerSegments
                                  where coalesce(customerSegments.segmentid, -1) =
                                    """ + " " + idsPart + " " + """
                    ))""";

            condition = condition.replace(wholePart, newValue);
        }


        return condition;
    }

    private String parseCustomerPreferencesVariable(String condition) {
        String regex = "\\$?(" + "CUSTOMER_PREFERENCES" + ")\\$?\\s*";
        condition = customerPreferencesVariableInOperators(condition, regex + "in\\s*\\((.*?)\\)");
        condition = customerPreferencesVariableEqualOperators(condition, regex + "(=\\s*\\$?\\d+\\$?)");
        condition = customerPreferencesVariableNotEqualOperators(condition, regex + "<>\\s*(\\$?\\d+\\$?)");
        return condition;
    }

    private String customerPreferencesVariableEqualOperators(String condition, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(2);
            String newValue = " " + """
                    (
                    (select COUNT(1) > 0
                     from (select cusPref.preferences_id as preferenceId
                           from service_contract.contract_details pdcDet
                                    join customer.customer_details cusDet
                                         on pdcDet.customer_detail_id = cusDet.id
                                    join customer.customer_preferences cusPref
                                         on cusPref.customer_detail_id = cusDet.id
                           where pdcDet.id = prodContDet.id) as customerPrefer
                     where coalesce(customerPrefer.preferenceId, -1)
                                    """ + " " + idsPart + " " + """
                    ))""";

            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    private String customerPreferencesVariableInOperators(String condition, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(2);
            String newValue = " " + """
                    (
                    (select COUNT(1) > 0
                     from (select cusPref.preferences_id as preferenceId
                           from service_contract.contract_details pdcDet
                                    join customer.customer_details cusDet
                                         on pdcDet.customer_detail_id = cusDet.id
                                    join customer.customer_preferences cusPref
                                         on cusPref.customer_detail_id = cusDet.id
                           where pdcDet.id = prodContDet.id) as customerPrefer
                     where coalesce(customerPrefer.preferenceId, -1) in(
                                    """ + " " + idsPart + " " + """
                    )))""";

            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    private String customerPreferencesVariableNotEqualOperators(String condition, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(2);
            String newValue = " " + """
                    (
                    (select COUNT(1) = 0
                     from (select cusPref.preferences_id as preferenceId
                           from service_contract.contract_details pdcDet
                                    join customer.customer_details cusDet
                                         on pdcDet.customer_detail_id = cusDet.id
                                    join customer.customer_preferences cusPref
                                         on cusPref.customer_detail_id = cusDet.id
                           where pdcDet.id = prodContDet.id) as customerPrefer
                     where coalesce(customerPrefer.preferenceId, -1) =
                                    """ + " " + idsPart + " " + """
                    ))""";

            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    private String parseDirectDebitVariable(String condition) {
        String regexIn = "\\$(DIRECT_DEBIT)\\$\\s?(in\\s*\\(.*?\\))";
        String regexOther = "\\$(DIRECT_DEBIT)\\$((=|<>)\\$(YES|NO)\\$)";
        condition = directDebitVariableMap(condition, regexIn, 2);
        condition = directDebitVariableMap(condition, regexOther, 2);

        return condition;
    }

    private String directDebitVariableMap(String condition, String regex, int groupId) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(groupId);
            idsPart = idsPart.replaceAll("YES", "TRUE");
            idsPart = idsPart.replaceAll("NO", "FALSE");
            String dbCondition = """
                    (select count(1) >0
                     where
                         ((select coalesce(custDet.direct_debit, FALSE) or coalesce(prodContDetal.direct_debit, FALSE)
                           from service_contract.contract_details prodContDetal
                                    join customer.customer_details custDet
                                         on prodContDetal.customer_detail_id = custDet.id
                                    join service_contract.contracts con
                                         on con.id = prodContDetal.contract_id
                           where prodContDetal.id = prodContDet.id)""" + " " + idsPart + " )) ";

            condition = condition.replace(wholePart, dbCondition);

        }

        return condition;
    }

    private String parsePerpetuityVariable(String condition) {
        condition = perpetuityVariableMap(condition, "\\$(CONTRACT_SUB_STATUS_IN_PERPETUITY)\\$\\s?(in\\s*\\(.*?\\))", 2);
        condition = perpetuityVariableMap(condition, "\\$?(CONTRACT_SUB_STATUS_IN_PERPETUITY)\\$?\\s*(=\\s*(\\$YES\\$|\\$NO\\$))", 2);
        condition = perpetuityVariableMap(condition, "\\$?(CONTRACT_SUB_STATUS_IN_PERPETUITY)\\$?\\s*(<>\\s*(\\$YES\\$|\\$NO\\$))", 2);
        return condition;
    }

    private String perpetuityVariableMap(String condition, String regex, int groupId) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(groupId);
            idsPart = idsPart.replaceAll("YES", "TRUE");
            idsPart = idsPart.replaceAll("NO", "FALSE");
            String newValue = " " + """
                    (select count(1) > 0
                     where (
                               (select pcc.contract_status = 'ACTIVE_IN_PERPETUITY' as statusCond
                                from service_contract.contracts as pcc
                                where pcc.id = prodContDet.contract_id)""" +
                    " " + idsPart + ") )";
            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    ///validations on redundant

    private String replaceRedundantConditions(String condition) {
        List<String> redundantVariableNames = new ArrayList<>(Arrays.stream(PriceComponentEvaluationPrimitiveVariableMapper.values())
                .filter(mapper -> !mapper.isCalculateFromServiceContract())
                .map(PriceComponentEvaluationPrimitiveVariableMapper::name)
                .toList());
        //add complex redundant variables
        redundantVariableNames.addAll(List.of("POD_REGION", "RISK_ASSESSMENT_ADDITIONAL_CONDITIONS"));

        String regex = "\\$?(" + String.join("|", redundantVariableNames) + ")\\$?\\s*";

        List<String> regexList = getRedundantVariablesRegexList(regex);
        for (String reg : regexList) {
            condition = matchRedundantVariable(condition, reg);
        }
        return condition;
    }

    private List<String> getRedundantVariablesRegexList(String regex) {
        List<String> regexList = new ArrayList<>();
        //ordering is matter first of all with not logical operators
        //not(=) for $ types
        regexList.add("NOT\\(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(=\\s*\\$.*?\\$)\\)");
        //not(<>) for $ types
        regexList.add("NOT\\(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(<>\\s*\\$.*?\\$)\\)");
        //not(in) for all types
        regexList.add("NOT\\(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(in\\s*\\((.*?)\\))\\)");
        //not(>) for only digits
        regexList.add("NOT\\(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(>\\s*\\d)\\)");
        //not(<) for only digits
        regexList.add("NOT\\(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(<\\s*\\d)\\)");
        //not(=) for only digits
        regexList.add("NOT\\(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(=\\s*\\d)\\)");
        //not(<>) for only digits
        regexList.add("NOT\\(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(<>\\s*\\d)\\)"); //not(=) for $ types

        //= for $ types
        regexList.add("\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(=\\s*\\$.*?\\$)");
        //<> for $ types
        regexList.add("\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(<>\\s*\\$.*?\\$)");
        //not(in) for all types
        regexList.add("\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(in\\s*\\((.*?)\\))");
        //> for only digits
        regexList.add("(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(>\\s*\\d))");
        //< for only digits
        regexList.add("(\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(<\\s*\\d))");
        //= for only digits
        regexList.add("\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(=\\s*\\d)");
        //<> for only digits
        regexList.add("\\$?(POD_VOLTAGE_LEVEL|POD_COUNTRY|PURPOSE_OF_CONSUMPTION|POD_POPULATED_PLACE|POD_GRID_OP|POD_PROVIDED_POWER|POD_MULTIPLIER|POD_MEASUREMENT_TYPE|CONTRACT_TYPE|POD_REGION|RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(<>\\s*\\d)");

        return regexList;
    }

    private String matchRedundantVariable(String condition, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            condition = condition.replace(wholePart, " TRUE ");
        }

        return condition;
    }

    private boolean validateAllNomenclatureValuesInCondition(String condition) {
        //get all nomenclature typed variable values from condition
        Map<String, Set<Long>> parsedConditionValuesMap = parseCondition(condition);

        Map<String, BiFunction<Long, List<NomenclatureItemStatus>, Boolean>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(CUSTOMER_SEGMENT.name(), segmentRepository::existsByIdAndStatusIn);
        repositoryLookups.put(CUSTOMER_PREFERENCES.toString(), preferencesRepository::existsByIdAndStatusIn);
        repositoryLookups.put(CONTRACT_CAMPAIGN.toString(), campaignRepository::existsByIdAndStatusIn);

        for (Map.Entry<String, Set<Long>> entry : parsedConditionValuesMap.entrySet()) {
            String key = entry.getKey();
            Set<Long> values = entry.getValue();
            if (repositoryLookups.containsKey(key)) {
                for (Long value : values) {
                    if (!repositoryLookups.get(key).apply(value, List.of(NomenclatureItemStatus.ACTIVE))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Map<String, Set<Long>> parseCondition(String condition) {
        Map<String, Set<Long>> resultMap = new HashMap<>();
        List<String> nomenclatureNames = Arrays.asList(
                CUSTOMER_SEGMENT.name(),
                CUSTOMER_PREFERENCES.name(),
                CONTRACT_CAMPAIGN.name()
        );
        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        parseConditionIds(condition, regex + "in\\s*\\(([^)]+)\\)", resultMap); // "in" conditions
        parseConditionIds(condition, regex + "=\\s*(\\$?\\d+\\$?)", resultMap); // "=" conditions
        parseConditionIds(condition, regex + "<>\\s*(\\$?\\d+\\$?)", resultMap); // "<>" conditions

        return resultMap;
    }

    private void parseConditionIds(String condition,
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
