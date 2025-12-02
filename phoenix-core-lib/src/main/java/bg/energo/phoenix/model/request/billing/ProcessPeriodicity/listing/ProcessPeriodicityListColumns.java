package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.listing;

import lombok.Getter;

public enum ProcessPeriodicityListColumns {
    NAME("name"),
    PERIODICITY("type"),
    CREATE_DATE("create_date");

    @Getter
    private final String value;

    ProcessPeriodicityListColumns(String periodicity) {
        this.value = periodicity;
    }
}
