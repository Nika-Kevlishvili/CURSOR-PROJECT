package bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationModel;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentPriceEvaluationResultModel;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.enums.PriceComponentFormulaComplexity;
import bg.energo.phoenix.service.product.price.priceComponent.priceEvaluation.strategy.AbstractPriceComponentPriceEvaluationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ognl.OgnlException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceComponentPriceEvaluationService {
    private final PriceComponentRepository priceComponentRepository;
    private final List<AbstractPriceComponentPriceEvaluationStrategy> evaluationStrategies;

    public List<BillingDataPriceComponentPriceEvaluationResultModel> evaluatePrice(BillingDataPriceComponentPriceEvaluationModel model, List<String> errorMessages) throws OgnlException {
        PriceComponent priceComponent = priceComponentRepository
                .findById(model.priceComponentId())
                .orElseThrow(() -> {
                    log.error("Price Component with presented id: [%s] not found;".formatted(model.priceComponentId()));
                    return new DomainEntityNotFoundException("Price Component with presented id: [%s] not found;".formatted(model.priceComponentId()));
                });

        try {
            PriceComponentFormulaComplexity priceComponentFormulaComplexity = AbstractPriceComponentPriceEvaluationStrategy.definePriceComponentFormulaComplexity(priceComponent.getPriceFormula());
            AbstractPriceComponentPriceEvaluationStrategy strategy = evaluationStrategies
                    .stream()
                    .filter(abstractPriceComponentPriceEvaluationStrategy -> abstractPriceComponentPriceEvaluationStrategy.getComplexity().equals(priceComponentFormulaComplexity))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentsProvidedException("Cannot found Price Component Evaluation strategy: [%s]".formatted(priceComponentFormulaComplexity)));

            return strategy.evaluate(priceComponent.getPriceFormula(), model);
        } catch (Exception e) {
            log.error("Exception handled while calculating Price Component Price", e);
            errorMessages.add(e.getMessage());
            throw e;
        }
    }
}
