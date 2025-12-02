package bg.energo.phoenix.model.response.contract.action.calculation;

import java.math.BigDecimal;

public interface ActionPenaltyPriceParameterEvaluationResult {
    Long getId();
    BigDecimal getAveragePrice();

    // The following variables are not needed in evaluation, but are helpful for debugging purposes
    Integer getCount();
    String getPeriodType();

    // This one is currently null, however, in case of needed, the query could be transformed
    // to return aggregated values for the price parameter detail info ids, that were included
    // in the evaluation process and thus, observing which ids were not included and why.
    String getPriceParamIds();
}
