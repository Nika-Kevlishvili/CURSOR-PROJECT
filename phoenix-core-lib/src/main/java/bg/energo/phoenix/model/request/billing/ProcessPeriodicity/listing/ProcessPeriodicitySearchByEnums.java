package bg.energo.phoenix.model.request.billing.ProcessPeriodicity.listing;

import lombok.Getter;

public enum ProcessPeriodicitySearchByEnums {
    ALL("ALL"),
    NAME("NAME"),
    PERIODICITY("PERIODICITY");

    @Getter
    private final String value;

    ProcessPeriodicitySearchByEnums(String value) {
        this.value = value;
    }
}
