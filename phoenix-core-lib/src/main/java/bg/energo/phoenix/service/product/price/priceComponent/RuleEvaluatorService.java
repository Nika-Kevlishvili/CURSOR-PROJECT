package bg.energo.phoenix.service.product.price.priceComponent;

import bg.energo.phoenix.model.request.product.price.RuleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluatorService {

    public boolean evaluateBooleanExpression(RuleRequest request) {
        try{
            return (boolean) (Boolean) Ognl.getValue(request.getExpression(), request.getArguments());
        } catch (Exception e) {
            log.error("Unable to evaluate rule", e);
            throw new IllegalArgumentException("Unable to evaluate rule");
        }
    }

}
