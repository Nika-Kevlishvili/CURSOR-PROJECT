package bg.energo.phoenix.model.response.priceParameter;

import java.math.BigDecimal;

public interface PriceParameterForCalculationResponse {
    Long getId();
    Boolean getPreviousThirtyDaysFilled();
    BigDecimal getTotalPrice();
    Long getTotalPeriodsWithinRange();
}
