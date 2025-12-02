package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.listing;

import lombok.Getter;

public enum ManualLiabilityOffsettingSearchByEnums {
    NUMBER("NUMBER"),
    CUSTOMER("CUSTOMER"),
    ALL("ALL");

    @Getter
    private final String value;

    ManualLiabilityOffsettingSearchByEnums(String value) {
        this.value = value;
    }
}
