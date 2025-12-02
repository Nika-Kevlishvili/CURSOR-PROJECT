package bg.energo.phoenix.billingRun.service;

import bg.energo.phoenix.billingRun.model.PriceComponentFormulaXMap;
import bg.energo.phoenix.billingRun.model.PriceComponentFormulaXValue;
import bg.energo.phoenix.billingRun.model.entity.BillingRunContracts;
import bg.energo.phoenix.billingRun.repository.BillingRunContractsRepository;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.repository.contract.product.ProductContractPriceComponentRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingDataByProfileRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationModel;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationResultModel;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.enums.PriceComponentFormulaComplexity;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.strategy.AbstractPriceComponentPriceEvaluationStrategy;
import ognl.OgnlException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BillingRunPriceComponentEvaluationService extends AbstractPriceComponentPriceEvaluationStrategy {

    private final BillingRunContractsRepository billingRunContractsRepository;

    public BillingRunPriceComponentEvaluationService(ExpressionStringParser expressionStringParser, PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository, BillingDataByProfileRepository billingDataByProfileRepository, PriceParameterDetailInfoRepository priceParameterDetailInfoRepository, ProductContractPriceComponentRepository productContractPriceComponentRepository, BillingRunContractsRepository billingRunContractsRepository) {
        super(expressionStringParser, priceComponentFormulaVariableRepository, billingDataByProfileRepository, priceParameterDetailInfoRepository, productContractPriceComponentRepository);
        this.billingRunContractsRepository = billingRunContractsRepository;
    }

    @Override
    public PriceComponentFormulaComplexity getComplexity() {
        return null;
    }

    @Override
    public List<BillingDataPriceComponentPriceEvaluationResultModel> evaluate(String priceFormula, BillingDataPriceComponentPriceEvaluationModel model) throws RuntimeException, OgnlException {
        return null;
    }
    public static String modifyExpression(String expression) {
        // Regular expression to match variables in the format $VAR$
        Pattern pattern = Pattern.compile("\\$(\\w+)\\$");
        Matcher matcher = pattern.matcher(expression);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variable = matcher.group(1); // Capture only the variable name (e.g., "X1", "X2")
            String modifiedVariable = "!$" + variable + "!$.doubleValue()"; // Escape $ properly

            // Use `Matcher.quoteReplacement()` to avoid illegal group reference issues
            matcher.appendReplacement(result, Matcher.quoteReplacement(modifiedVariable));
        }
        matcher.appendTail(result);

        return result.toString().replace("!","");
    }

    public BigDecimal evaluateExpression(String formula, Map<String, Object> variablesContext) throws OgnlException {
        String modifiedExpression = modifyExpression(formula);
        BigDecimal price = super.evaluateExpression(modifiedExpression, variablesContext);
        if (price == null) {
            throw new RuntimeException("price cannot not be evaluated, invalid formula");
        }
        return price.setScale(12, RoundingMode.HALF_UP);
    }

    public List<PriceComponentFormulaXValue> getFormulaXValues(Long contractDetailId, Long priceComponentId, HashMap<PriceComponentFormulaXMap, List<PriceComponentFormulaXValue>> xValues, BillingRunContracts runContract) {
        PriceComponentFormulaXMap map = new PriceComponentFormulaXMap(contractDetailId, priceComponentId);
        List<PriceComponentFormulaXValue> formulaXValues = xValues.getOrDefault(map, null);
        if (formulaXValues != null) {
            return formulaXValues;
        }

        if (runContract.getContractType().equals(ContractType.PRODUCT_CONTRACT))
            formulaXValues = billingRunContractsRepository.getPriceComponentXValuesForProductContracts(contractDetailId, priceComponentId);
        else
            formulaXValues = billingRunContractsRepository.getPriceComponentXValuesForServiceContracts(contractDetailId, priceComponentId);

        xValues.put(map, formulaXValues);

        return formulaXValues;
    }
}
