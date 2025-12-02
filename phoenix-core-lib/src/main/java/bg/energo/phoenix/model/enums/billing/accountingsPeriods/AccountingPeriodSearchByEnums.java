package bg.energo.phoenix.model.enums.billing.accountingsPeriods;

import lombok.Getter;

/**
 * <h1>AccountingPeriodSearchBy Enum</h1>
 * {@link #NAME}
 * {@link #ALL}
 */
public enum AccountingPeriodSearchByEnums {
    NAME("NAME"),
    ALL("ALL");

    @Getter
    private final String value;

    AccountingPeriodSearchByEnums(String value) {
        this.value = value;
    }
}
