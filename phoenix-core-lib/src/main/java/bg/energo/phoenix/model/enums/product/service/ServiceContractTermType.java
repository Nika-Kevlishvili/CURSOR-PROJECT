package bg.energo.phoenix.model.enums.product.service;

import lombok.Getter;

import java.time.temporal.ChronoUnit;

public enum ServiceContractTermType {
    DAY_DAYS(ChronoUnit.DAYS),
    MONTH_MONTHS(ChronoUnit.MONTHS),
    YEAR_YEARS(ChronoUnit.YEARS);
    @Getter
    private ChronoUnit unit;

    ServiceContractTermType(ChronoUnit chronoUnit) {
        this.unit=chronoUnit;
    }
}
