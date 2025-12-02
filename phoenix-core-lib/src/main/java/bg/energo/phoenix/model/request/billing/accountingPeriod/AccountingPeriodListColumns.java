package bg.energo.phoenix.model.request.billing.accountingPeriod;

import lombok.Getter;

public enum AccountingPeriodListColumns {
    NAME("name"),
    START_DATE("start_date"),
    END_DATE("end_date"),
    STATUS("status"),
    CLOSED_ON_DATE("closed_on_date");

    @Getter
    private String value;

    AccountingPeriodListColumns(String value) {
        this.value = value;
    }
}
