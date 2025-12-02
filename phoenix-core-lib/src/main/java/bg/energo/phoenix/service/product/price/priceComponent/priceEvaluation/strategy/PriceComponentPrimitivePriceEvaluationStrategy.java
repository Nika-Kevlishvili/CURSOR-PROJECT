package bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.strategy;

import bg.energo.phoenix.model.entity.contract.product.ProductContractPriceComponents;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponentFormulaVariable;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.repository.contract.product.ProductContractPriceComponentRepository;
import bg.energo.phoenix.repository.pod.billingByProfile.BillingDataByProfileRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentFormulaVariableRepository;
import bg.energo.phoenix.repository.product.price.priceParameter.PriceParameterDetailInfoRepository;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationModel;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationResultModel;
import bg.energo.phoenix.service.product.price.priceComponent.ExpressionStringParser;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.enums.PriceComponentFormulaComplexity;
import ognl.OgnlException;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PriceComponentPrimitivePriceEvaluationStrategy extends AbstractPriceComponentPriceEvaluationStrategy {
    public PriceComponentPrimitivePriceEvaluationStrategy(PriceComponentFormulaVariableRepository priceComponentFormulaVariableRepository,
                                                          ExpressionStringParser expressionStringParser,
                                                          BillingDataByProfileRepository billingDataByProfileRepository,
                                                          PriceParameterDetailInfoRepository priceParameterDetailInfoRepository,
                                                          ProductContractPriceComponentRepository productContractPriceComponentRepository) {
        super(expressionStringParser, priceComponentFormulaVariableRepository, billingDataByProfileRepository, priceParameterDetailInfoRepository, productContractPriceComponentRepository);
    }

    @Override
    public PriceComponentFormulaComplexity getComplexity() {
        return PriceComponentFormulaComplexity.PRIMITIVE;
    }

    @Override
    public List<BillingDataPriceComponentPriceEvaluationResultModel> evaluate(String formula,
                                                                              BillingDataPriceComponentPriceEvaluationModel model) throws RuntimeException, OgnlException {
        Map<String, Object> variablesContext = new HashMap<>();

        List<PriceComponentFormulaVariable> priceComponentFormulaVariables = priceComponentFormulaVariableRepository
                .findAllByPriceComponentIdOrderByIdAsc(model.priceComponentId());
        List<ProductContractPriceComponents> productContractPriceComponents = productContractPriceComponentRepository
                .findByContractDetailIdAndStatusIn(model.productContractDetailId(), List.of(ContractSubObjectStatus.ACTIVE));

        putProductContractFormulaVariablesInContext(model, variablesContext, productContractPriceComponents);
        putPriceComponentFormulaVariablesInContext(model, variablesContext, priceComponentFormulaVariables);

        LocalDateTime periodFrom = model.dateFrom() == null ? null : adjustDateFromByDimension(model.dateFrom(), model.dimension());
        LocalDateTime periodTo = model.dateTo() == null ? null : adjustDateToByDimension(model.dateFrom(), model.dimension());
        if (NumberUtils.isParsable(formula)) {
            return List.of(new BillingDataPriceComponentPriceEvaluationResultModel(periodFrom, periodTo, null, new BigDecimal(formula)));
        } else {
            BigDecimal result = evaluateExpression(formula, variablesContext);
            return List.of(new BillingDataPriceComponentPriceEvaluationResultModel(periodFrom, periodTo, null, result));
        }
    }
}
