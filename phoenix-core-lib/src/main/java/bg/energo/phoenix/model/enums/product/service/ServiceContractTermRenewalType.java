package bg.energo.phoenix.model.enums.product.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.temporal.ChronoUnit;

@Getter
@AllArgsConstructor
public enum ServiceContractTermRenewalType {

    DAY_DAYS(ChronoUnit.DAYS),
    MONTH_MONTHS(ChronoUnit.MONTHS),
    YEAR_YEARS(ChronoUnit.YEARS);

    private final ChronoUnit unit;

}
