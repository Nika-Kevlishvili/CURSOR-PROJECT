package bg.energo.phoenix.model.enums.billing.accountingsPeriods;

import lombok.Getter;

/**
 * <h1>AccountingPeriodStatus Enum</h1>
 * {@link #OPEN}
 * {@link #CLOSED}
 */
public enum AccountingPeriodStatus {
    OPEN("OPEN"),
    CLOSED("CLOSED");

    @Getter
    private final String value;

    AccountingPeriodStatus(String value) {
        this.value = value;
    }
}
