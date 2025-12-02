package bg.energo.phoenix.service.billing.runs.services.evaluatePriceComponentCondition;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableValue;
import bg.energo.phoenix.repository.nomenclature.address.CountryRepository;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.repository.nomenclature.address.RegionRepository;
import bg.energo.phoenix.repository.nomenclature.billing.RiskAssessmentRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.customer.PreferencesRepository;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.pod.PodAdditionalParametersRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
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
import static bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableName.POD_GRID_OP;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluateProductContractConditionService {

    private final PriceComponentRepository priceComponentRepository;
    private final PreferencesRepository preferencesRepository;
    private final CountryRepository countryRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final RegionRepository regionRepository;
    private final CampaignRepository campaignRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final SegmentRepository segmentRepository;
    private final SqlCommandValidationService sqlCommandValidationService;
    private final PodAdditionalParametersRepository podAdditionalParametersRepository;


    @PersistenceContext
    private EntityManager em;

    public List<TempPriceConditionModel> evaluateConditionWithQuery(List<TempPriceConditionModel> billingDataInvoiceModels) {
        List<TempPriceConditionModel> finalList = new ArrayList<>();
        //group data by priceComponentId
        Map<Long, List<TempPriceConditionModel>> billingDataInvoiceModelsByPriceComponent = billingDataInvoiceModels
                .stream()
                .collect(groupingBy(TempPriceConditionModel::getPriceComponentId, toList()));
        for (Long priceComponentId : billingDataInvoiceModelsByPriceComponent.keySet()) {
            //TODO check on active status
            Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findById(priceComponentId);
            //if price Component does not exist with  priceComponentId id should ignore all data with this price component
            if (priceComponentOptional.isEmpty()) {
                log.error("there is no price component with id %s".formatted(priceComponentId));
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

    public boolean isConditionHasSqlCommands(String condition) {
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
        condition = condition.replace("str1ng_to_array", "string_to_array");
        condition = condition.replace("cpho.contract_bill1ng_group_id", "cpho.contract_billing_group_id");

        //after all variable name changed with table and column names, set condition into query
        String finalQuery = querySelectFrom + " AND (" + condition + ")";
        getResultFromDbWithGeneratedQuery(finalQuery, finalList, priceComponentId);
    }


    private void getResultFromDbWithGeneratedQuery(String finalQuery, List<TempPriceConditionModel> finalList, Long priceComponentId) {
        try {
            List<Object[]> result = em.createNativeQuery(finalQuery).getResultList();
            result.forEach(obj -> {
                TempPriceConditionModel tempPriceConditionModel = new TempPriceConditionModel();
                tempPriceConditionModel.setContractDetailId(((BigInteger) obj[0]).longValue());
                tempPriceConditionModel.setPodDetailId(((BigInteger) obj[1]).longValue());
                tempPriceConditionModel.setPodId(((BigInteger) obj[2]).longValue());
                tempPriceConditionModel.setPriceComponentId(priceComponentId);
                finalList.add(tempPriceConditionModel);
            });
        } catch (Exception e) {
            log.error("Error executing query: {}", finalQuery, e);
        }
    }

    private String generateQuerySelectPart(List<TempPriceConditionModel> tempPriceConditionModels) {
        String querySelectFrom = """
                select prodcontDet.id as contractDetailId, podDet.id as podDetailId, pod.id as podId
                from product_contract.contract_details prodContDet
                         join customer.customer_details custDet
                              on prodContDet.customer_detail_id = custDet.id
                         join customer.customers cust
                              on cust.id = custDet.customer_id
                         join product_contract.contract_pods prodContrPods
                              on prodContrPods.contract_detail_id = prodContDet.id
                         join pod.pod_details podDet
                              on podDet.id = prodContrPods.pod_detail_id
                         join pod.pod pod
                              on pod.id = podDet.pod_id
                where prodContDet.id in (:productContractIdsArg)
                  and prodContDet.status = 'SIGNED'
                  and podDet.id in (:podDetailIdsArg)""";

        List<String> productContractIds = tempPriceConditionModels.stream().map(model -> String.valueOf(model.getContractDetailId())).toList();
        List<String> podDetailIds = tempPriceConditionModels.stream().map(model -> String.valueOf(model.getPodDetailId())).toList();

        querySelectFrom = querySelectFrom.replace(":productContractIdsArg", String.join(",", productContractIds));
        querySelectFrom = querySelectFrom.replace(":podDetailIdsArg", String.join(",", podDetailIds));

        return querySelectFrom;
    }

    private String appendConditionQueryForComplexVariables(String condition) {
        if (condition.contains("CUSTOMER_SEGMENT")) {
            condition = parseCustomerSegmentVariable(condition);
        }
        if (condition.contains("CUSTOMER_PREFERENCES")) {
            condition = parseCustomerPreferencesVariable(condition);
        }
        if (condition.contains("POD_REGION")) {
            condition = parsePodRegionVariable(condition);
        }
        if (condition.contains("DIRECT_DEBIT")) {
            condition = parseDirectDebitVariable(condition);
        }
        if (condition.contains("CONTRACT_SUB_STATUS_IN_PERPETUITY")) {
            condition = parsePerpetuityVariable(condition);
        }
        if (condition.contains("RISK_ASSESSMENT_ADDITIONAL_CONDITIONS")) {
            condition = parseRiskAssVariable(condition);
        }

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
        condition = condition.replace("MED1UM_DIRECT_CONNECTED", "\'MEDIUM_DIRECT_CONNECTED\'");
        return condition;
    }

    private String improveConditionQuerySyntax(String condition) {
        condition = condition.replace("$", " ");
        condition = condition.replace("AND", " AND ");
        condition = condition.replace("OR", " OR ");
        condition = condition.replace("join", " job1n ");
        condition = condition.replace("product_contract.contract_billing_groups", "product_contract.contract_bill1ng_groups");
        condition = condition.replace("cpho.contract_billing_group_id", "cpho.contract_bill1ng_group_id");
        condition = condition.replace("string_to_array", "str1ng_to_array");
        condition = condition.replace("in", " in ");
        condition = condition.replace("NOT", " NOT ");
        condition = condition.replace("NON_HOUSEHOLD", "NON_H1OUSEHOLD");
        condition = condition.replace("MEDIUM_DIRECT_CONNECTED", "MED1UM_DIRECT_CONNECTED");

        return condition;
    }

    private String parseRiskAssVariable(String condition) {
        condition = riskAssVariableMap(condition, "\\$?(RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(in\\s*\\((.*?)\\))", 3);
        condition = riskAssVariableMap(condition, "\\$?(RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(=(\\s*\\$?\\d+\\$?))", 3);
        condition = riskAssNotEqualVariableMap(condition, "\\$?(RISK_ASSESSMENT_ADDITIONAL_CONDITIONS)\\$?\\s*(<>(\\s*\\$?\\d+\\$?))", 3);
        return condition;
    }

    private String riskAssVariableMap(String condition, String regex, int groupId) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(groupId);
            String newValue = " " + """
                    (cast((select array_agg(name)
                                 from nomenclature.risk_assessments
                                 where id in (""" + idsPart + "))" + """
                    as text[])
                    && (select string_to_array(risk_assessment_additional_condition, ';')
                        from product_contract.contract_details cdt
                        where cdt.id = prodContDet.id)
                          )""";
            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    private String riskAssNotEqualVariableMap(String condition, String regex, int groupId) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(groupId);
            String newValue = " " + """
                    (not (cast((select array_agg(name)
                                 from nomenclature.risk_assessments
                                 where id in (""" + idsPart + "))" + """
                    as text[])
                    && (select string_to_array(risk_assessment_additional_condition, ';')
                        from product_contract.contract_details cdt
                        where cdt.id = prodContDet.id)
                          ))""";
            condition = condition.replace(wholePart, newValue);
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
                                from product_contract.contracts as pcc
                                where pcc.id = prodContDet.contract_id)""" +
                    " " + idsPart + ") )";
            condition = condition.replace(wholePart, newValue);
        }

        return condition;
    }

    private String parsePodRegionVariable(String condition) {

        condition = podRegionVariableMap(condition, "\\$?(POD_REGION)\\$?\\s*(in\\s*\\((.*?)\\))", 2);
        condition = podRegionVariableMap(condition, "\\$?(POD_REGION)\\$?\\s*(=\\s*\\$?\\d+\\$?)", 2);
        condition = podRegionVariableMap(condition, "\\$?(POD_REGION)\\$?\\s*(<>\\s*(\\$?\\d+\\$?))", 2);

        return condition;
    }

    private String podRegionVariableMap(String condition, String regex, int groupId) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            String wholePart = matcher.group(0);
            String idsPart = matcher.group(groupId);
            String newValue = " " + """
                    (select count(1) >0 from nomenclature.populated_places as popPlace
                    join nomenclature.municipalities as mun
                    on popPlace.municipality_id = mun.id
                    join nomenclature.regions as reg
                    on mun.region_id = reg.id
                    where popPlace.id = podDet.populated_place_id and reg.id """ +
                    " " + idsPart + ") ";
            condition = condition.replace(wholePart, newValue);
        }
        return condition;
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
                                        from product_contract.contract_details pdcDet
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
                           from product_contract.contract_details pdcDet
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
                                        from product_contract.contract_details pdcDet
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
                           from product_contract.contract_details pdcDet
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
                           from product_contract.contract_details pdcDet
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
                           from product_contract.contract_details pdcDet
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
                         ((select coalesce(custDet.direct_debit, FALSE) or coalesce(prodContDetal.direct_debit, FALSE) or coalesce(contBillGr.direct_debit, FALSE) as directStatus
                          from product_contract.contract_details prodContDetal
                                   join  customer.customer_details custDet
                                         on prodContDetal.customer_detail_id = custDet.id
                                   join  product_contract.contracts con
                                         on con.id = prodContDetal.contract_id
                                   join product_contract.contract_pods as cpho
                                        on cpho.pod_detail_id = podDet.id
                                   join  product_contract.contract_billing_groups contBillGr
                                         on contBillGr.id = cpho.contract_billing_group_id
                          where prodContDetal.id = prodContDet.id and cpho.contract_detail_id = prodContDet.id)""" + " " + idsPart + " )) ";

            condition = condition.replace(wholePart, dbCondition);

        }


        return condition;
    }


    ///validation methods
    private boolean validateAllNomenclatureValuesInCondition(String condition) {
        //get all nomenclature typed variable values from condition
        Map<String, Set<Long>> parsedConditionValuesMap = parseCondition(condition);

        Map<String, BiFunction<Long, List<NomenclatureItemStatus>, Boolean>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(CUSTOMER_SEGMENT.name(), segmentRepository::existsByIdAndStatusIn);
        repositoryLookups.put(RISK_ASSESSMENT_ADDITIONAL_CONDITIONS.toString(), riskAssessmentRepository::existsByIdAndStatusIn);
        repositoryLookups.put(CUSTOMER_PREFERENCES.toString(), preferencesRepository::existsByIdAndStatusIn);
        repositoryLookups.put(POD_COUNTRY.toString(), countryRepository::existsByIdAndStatusIn);
        repositoryLookups.put(POD_POPULATED_PLACE.toString(), populatedPlaceRepository::existsByIdAndStatusIn);
        repositoryLookups.put(POD_REGION.toString(), regionRepository::existsByIdAndStatusIn);
        repositoryLookups.put(POD_GRID_OP.toString(), gridOperatorRepository::existsByIdAndStatusIn);
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
                POD_COUNTRY.name(),
                POD_REGION.name(),
                POD_POPULATED_PLACE.name(),
                POD_GRID_OP.name(),
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
