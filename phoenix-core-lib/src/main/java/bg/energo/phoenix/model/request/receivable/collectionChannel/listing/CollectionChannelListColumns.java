package bg.energo.phoenix.model.request.receivable.collectionChannel.listing;

import lombok.Getter;

public enum CollectionChannelListColumns {
    ID("id"),
    NAME("name"),
    COLLECTION_PARTNER("collectionPartner"),
    TYPE("type"),
    CURRENCY("currency");

    @Getter
    private final String value;

    CollectionChannelListColumns(String collectionChannelColumn) {
        this.value = collectionChannelColumn;
    }
}
