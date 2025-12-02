package bg.energo.phoenix.model.request.receivable.manualLiabilityOffsetting.listing;

import lombok.Getter;

public enum ManualLiabilityOffsettingListColumns {
    ID("id"),
    DATE("manual_liabilitie_date"),
    CUSTOMER("customer_id"),
    REVERSED("reversed");

    @Getter
    private final String value;

    ManualLiabilityOffsettingListColumns(String collectionChannelColumn) {
        this.value = collectionChannelColumn;
    }
}
