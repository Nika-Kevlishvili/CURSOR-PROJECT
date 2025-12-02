package bg.energo.phoenix.service.product.price.priceComponent;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.entity.product.price.priceComponent.ProfileForBalancing;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameter;
import bg.energo.phoenix.model.entity.product.price.priceParameter.PriceParameterDetails;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableName;
import bg.energo.phoenix.model.request.product.price.priceComponent.AlternateInvoiceRecipient;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentPriceTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.priceComponent.PriceComponentValueTypeResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentDetailedResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceFormulaPreviewInfo;
import bg.energo.phoenix.model.response.priceComponent.ProfileForBalancingShortResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.address.CountryRepository;
import bg.energo.phoenix.repository.nomenclature.address.PopulatedPlaceRepository;
import bg.energo.phoenix.repository.nomenclature.address.RegionRepository;
import bg.energo.phoenix.repository.nomenclature.billing.RiskAssessmentRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.customer.PreferencesRepository;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.pod.PodAdditionalParametersRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailsRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableForParsing.*;
import static bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableName.POD_ADDITIONAL_PARAMETERS;
import static bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentConditionVariableName.POD_GRID_OP;

@Service
@RequiredArgsConstructor
public class PriceComponentMapper {
    private final PriceParameterRepository priceParameterRepository;
    private final PriceParameterDetailsRepository priceParameterDetailsRepository;
    private final SegmentRepository segmentRepository;
    private final PreferencesRepository preferencesRepository;
    private final CountryRepository countryRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final RegionRepository regionRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final PodAdditionalParametersRepository podAdditionalParametersRepository;

    /**
     * Parse the condition string for nomenclature occurrences and returns a map of the condition types and their values.
     *
     * @param condition The condition string to parse
     * @return A map of the variables and their values aggregated in a list of IDs
     */
    private static Map<String, Set<Long>> parseCondition(String condition) {
        Map<String, Set<Long>> resultMap = new HashMap<>();

        // Every new nomenclature (if added to types) should also be added to the regex
        List<String> nomenclatureNames = Arrays.asList(
                CUSTOMER_SEGMENT.name(),
                CUSTOMER_PREFERENCES.name(),
                POD_COUNTRY.name(),
                POD_REGION.name(),
                POD_POPULATED_PLACE.name(),
                POD_GRID_OP.name(),
                CONTRACT_CAMPAIGN.name(),
                RISK_ASSESSMENT_ADDITIONAL_CONDITIONS.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        parseConditionType(condition, regex + "in\\s*\\(([^)]+)\\)", resultMap); // "in" conditions
        parseConditionType(condition, regex + "=\\s*(\\$?\\d+\\$?)", resultMap); // "=" conditions
        parseConditionType(condition, regex + "<>\\s*(\\$?\\d+\\$?)", resultMap); // "<>" conditions

        return resultMap;
    }

    public boolean validateConditionLogicalOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.asList(
                PriceComponentConditionVariableName.CUSTOMER_TYPE.name(),
                PriceComponentConditionVariableName.CUSTOMER_SEGMENT.name(),
                PriceComponentConditionVariableName.CUSTOMER_PREFERENCES.name(),
                PriceComponentConditionVariableName.POD_COUNTRY.name(),
                PriceComponentConditionVariableName.POD_REGION.name(),
                PriceComponentConditionVariableName.POD_POPULATED_PLACE.name(),
                PriceComponentConditionVariableName.POD_GRID_OP.name(),
                PriceComponentConditionVariableName.POD_VOLTAGE_LEVEL.name(),
                PriceComponentConditionVariableName.POD_MEASUREMENT_TYPE.name(),
                PriceComponentConditionVariableName.DIRECT_DEBIT.name(),
                PriceComponentConditionVariableName.CONTRACT_SUB_STATUS_IN_PERPETUITY.name(),
                PriceComponentConditionVariableName.ACTIVE_POWER_SUPPLY_TERMINATION.name(),
                PriceComponentConditionVariableName.CONTRACT_TYPE.name(),
                PriceComponentConditionVariableName.RISK_ASSESSMENT_ADDITIONAL_CONDITIONS.name(),
                PriceComponentConditionVariableName.CONTRACT_CAMPAIGN.name(),
                PriceComponentConditionVariableName.PURPOSE_OF_CONSUMPTION.name(),
                PriceComponentConditionVariableName.POD_ADDITIONAL_PARAMETERS.name()
        );

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, regex + ">\\s*(\\$?\\.*+\\$?)", result, errorMessages, "formulaRequest.condition- [condition] Illegal logical operator with variable"); // ">" conditions
        validateLogicalOperator(condition, regex + "<(\\$\\.*+\\$?)", result, errorMessages, "formulaRequest.condition- [condition] Illegal logical operator with variable"); // "<" conditions

        return result.isEmpty();
    }

    public boolean validateConditionNotOperatorType(String condition, List<String> errorMessages) {
        List<String> result = new ArrayList<>();

        List<String> nomenclatureNames = Arrays.stream(PriceComponentConditionVariableName.values()).map(PriceComponentConditionVariableName::name).toList();

        String regex = "\\$?(" + String.join("|", nomenclatureNames) + ")\\$?\\s*";

        validateLogicalOperator(condition, "NOT" + regex, result, errorMessages, "formulaRequest.condition- [condition] Illegal syntax of NOT operator with variable"); // "NOT" conditions

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

    /**
     * Parse the condition string for a specific condition type and add the values to the resultMap.
     * Suitable only for parsing the condition string for nomenclature occurrences.
     *
     * @param condition The condition string to parse
     * @param regex     The regex to use for parsing
     * @param resultMap The map to add the values to
     */
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

    public static boolean isDigit(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public PriceComponentDetailedResponse toDetailedResponse(PriceComponent priceComponent, ApplicationModelResponse applicationModel) {
        List<PriceFormulaPreviewInfo> priceFormulaInfo = new ArrayList<>();
        replaceIdWithName(priceComponent.getPriceFormula(), priceFormulaInfo);

        AlternateInvoiceRecipient alternateInvoiceRecipient = new AlternateInvoiceRecipient();
        if (priceComponent.getAlternativeRecipientCustomerDetailId() != null) {
            CustomerDetails customerDetails = customerDetailsRepository.findById(priceComponent.getAlternativeRecipientCustomerDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find CustomerDetail with id: %s".formatted(priceComponent.getAlternativeRecipientCustomerDetailId())));
            Customer customer = customerRepository.getReferenceById(customerDetails.getCustomerId());
            alternateInvoiceRecipient.setIdentifier(customer.getIdentifier());
            alternateInvoiceRecipient.setCustomerId(customerDetails.getCustomerId());
            alternateInvoiceRecipient.setStartDate(customerDetails.getCreateDate());
            alternateInvoiceRecipient.setVersionId(customerDetails.getVersionId());
            alternateInvoiceRecipient.setCustomerName(customerDetails.getName());
        }

        PriceComponentDetailedResponse priceComponentDetailedResponse = PriceComponentDetailedResponse.builder()
                .id(priceComponent.getId())
                .applicationModelResponse(applicationModel)
                .conditions(priceComponent.getConditions())
                .conditionsInfo(getConditionsInfo(priceComponent.getConditions()))
                .contractTemplateTag(priceComponent.getContractTemplateTag())
                .costCenterControllingOrder(priceComponent.getCostCenterControllingOrder())
                .currency(new CurrencyResponse(priceComponent.getCurrency()))
                .formulaVariables(toFormulaVariables(priceComponent.getFormulaVariables()))
                .globalVatRate(priceComponent.getGlobalVatRate())
                .discount(priceComponent.getDiscount())
                .incomeAccountNumber(priceComponent.getIncomeAccountNumber())
                .invoiceAndTemplateText(priceComponent.getInvoiceAndTemplateText())
                .issuedSeparateInvoice(priceComponent.getIssuedSeparateInvoice())
                .name(priceComponent.getName())
                .numberType(priceComponent.getNumberType())
                .priceInWords(priceComponent.getPriceInWords())
                .status(priceComponent.getStatus())
                .priceFormula(priceComponent.getPriceFormula())
                .priceFormulaInfo(priceFormulaInfo)
                .priceType(new PriceComponentPriceTypeResponse(priceComponent.getPriceType()))
                .valueType(new PriceComponentValueTypeResponse(priceComponent.getValueType()))
                .contractTemplateTag(priceComponent.getContractTemplateTag())
                .alternateInvoiceRecipient(alternateInvoiceRecipient)
                .doNotIncludeVatBase(priceComponent.getDoNotIncludeInTheVatBase())
                .xenergieApplicationType(priceComponent.getXenergieApplicationType())
                .build();

        if (priceComponent.getVatRate() != null) {
            priceComponentDetailedResponse.setVatRate(new VatRateResponse(priceComponent.getVatRate()));
        }

        return priceComponentDetailedResponse;
    }

    /**
     * Parse the condition string and return a list of PriceFormulaPreviewInfo objects populated with the
     * information about the nomenclature values used in the condition.
     *
     * @param condition The condition string to parse
     * @return A list of PriceFormulaPreviewInfo objects
     */
    private List<PriceFormulaPreviewInfo> getConditionsInfo(String condition) {
        if (StringUtils.isEmpty(condition)) {
            return Collections.emptyList();
        }

        List<PriceFormulaPreviewInfo> conditionsInfo = new ArrayList<>();
        Map<String, Set<Long>> parsedConditionValuesMap = parseCondition(condition);

        Map<String, Function<List<Long>, List<ActivityNomenclatureResponse>>> repositoryLookups = new HashMap<>();
        repositoryLookups.put(CUSTOMER_SEGMENT.name(), segmentRepository::findByIdIn);
        repositoryLookups.put(CUSTOMER_PREFERENCES.toString(), preferencesRepository::findByIdIn);
        repositoryLookups.put(POD_COUNTRY.toString(), countryRepository::findByIdIn);
        repositoryLookups.put(POD_REGION.toString(), regionRepository::findByIdIn);
        repositoryLookups.put(POD_POPULATED_PLACE.toString(), populatedPlaceRepository::findByIdIn);
        repositoryLookups.put(POD_GRID_OP.toString(), gridOperatorRepository::findByIdIn);
        repositoryLookups.put(CONTRACT_CAMPAIGN.toString(), campaignRepository::findByIdIn);
        repositoryLookups.put(RISK_ASSESSMENT_ADDITIONAL_CONDITIONS.toString(), riskAssessmentRepository::findByIdIn);
        repositoryLookups.put(POD_ADDITIONAL_PARAMETERS.toString(), podAdditionalParametersRepository::findByIdIn);

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

    /**
     * Add the condition information to the list of PriceFormulaPreviewInfo objects.
     *
     * @param entry          The entry from the parsed condition map
     * @param valueName      The name of the value
     * @param valueId        The id of the value
     * @param conditionsInfo The list of PriceFormulaPreviewInfo objects
     */
    private void addConditionInfoToList(Map.Entry<String, Set<Long>> entry,
                                        String valueName,
                                        Long valueId,
                                        List<PriceFormulaPreviewInfo> conditionsInfo) {
        PriceFormulaPreviewInfo priceFormulaPreviewInfo = new PriceFormulaPreviewInfo();
        priceFormulaPreviewInfo.setKey(entry.getKey());
        priceFormulaPreviewInfo.setName(valueName);
        priceFormulaPreviewInfo.setId(valueId);
        conditionsInfo.add(priceFormulaPreviewInfo);
    }

    public void replaceIdWithName(String string, List<PriceFormulaPreviewInfo> priceFormulaInfo) {
        Pattern pattern = Pattern.compile("\\$([^\\$]+)\\$");
        Matcher matcher = pattern.matcher(string);

        StringBuilder replacedStringBuilder = new StringBuilder();
        while (matcher.find()) {
            String parameter = matcher.group(1);
            if (isDigit(parameter)) {
                PriceFormulaPreviewInfo priceFormulaPreviewInfo = new PriceFormulaPreviewInfo();
                String replacement = getParameterValue(parameter);
                priceFormulaPreviewInfo.setId(Long.valueOf(parameter));
                priceFormulaPreviewInfo.setName(replacement);
                priceFormulaInfo.add(priceFormulaPreviewInfo);
                matcher.appendReplacement(replacedStringBuilder, replacement);
            }
        }
        matcher.appendTail(replacedStringBuilder);

    }

    private String getParameterValue(String parameter) {
        PriceParameter priceParameter = priceParameterRepository.findById(Long.valueOf(parameter))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find PriceParameter with id: %s".formatted(parameter)));
        PriceParameterDetails priceParameterDetails = priceParameterDetailsRepository.findById(priceParameter.getLastPriceParameterDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find PriceParameterDetails with id: %s".formatted(parameter)));

        return priceParameterDetails.getName();
    }

    private List<PriceComponentDetailedResponse.FormulaVariablePayload> toFormulaVariables(List<PriceComponentFormulaVariable> formulaVariables) {
        return formulaVariables.stream()
                .map(formulaVariable -> {
                    ProfileForBalancing profileForBalancing = formulaVariable.getProfileForBalancing();
                    return PriceComponentDetailedResponse.FormulaVariablePayload
                            .builder()
                            .id(formulaVariable.getId())
                            .variable(formulaVariable.getVariable().name())
                            .description(formulaVariable.getDescription())
                            .value(formulaVariable.getValue())
                            .valueFrom(formulaVariable.getValueFrom())
                            .valueTo(formulaVariable.getValueTo())
                            .balancingProfileName(Objects.isNull(profileForBalancing) ? null : new ProfileForBalancingShortResponse(profileForBalancing))
                            .build();
                })
                .toList();
    }
}
