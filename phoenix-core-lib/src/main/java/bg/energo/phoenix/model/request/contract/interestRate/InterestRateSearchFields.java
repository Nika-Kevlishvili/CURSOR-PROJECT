package bg.energo.phoenix.model.request.contract.interestRate;

import lombok.Getter;

public enum InterestRateSearchFields {
    ALL("ALL"),
    ID("ID"),
    NAME("NAME");

    @Getter
    private final String value;

    InterestRateSearchFields(String value) {
        this.value = value;
    }
}
