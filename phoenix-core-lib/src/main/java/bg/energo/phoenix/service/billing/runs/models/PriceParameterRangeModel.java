package bg.energo.phoenix.service.billing.runs.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PriceParameterRangeModel {

    BigDecimal getPrice();
    LocalDateTime getPeriodFrom();
    LocalDateTime getPeriodTo();
    Boolean getIsShiftedHour();

}
