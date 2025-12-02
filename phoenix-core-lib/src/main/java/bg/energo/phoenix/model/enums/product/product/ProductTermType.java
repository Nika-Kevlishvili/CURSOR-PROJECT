package bg.energo.phoenix.model.enums.product.product;

import lombok.Getter;

import java.time.temporal.ChronoUnit;

public enum ProductTermType {
    DAY_DAYS(ChronoUnit.DAYS),
    MONTH_MONTHS(ChronoUnit.MONTHS),
    YEAR_YEARS(ChronoUnit.YEARS);
    @Getter
    private ChronoUnit unit;

    ProductTermType(ChronoUnit chronoUnit) {
        this.unit=chronoUnit;
    }
}
