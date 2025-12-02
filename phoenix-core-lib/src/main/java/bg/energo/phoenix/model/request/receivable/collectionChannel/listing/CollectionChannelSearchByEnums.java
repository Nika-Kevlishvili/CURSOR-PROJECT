package bg.energo.phoenix.model.request.receivable.collectionChannel.listing;

import lombok.Getter;

public enum CollectionChannelSearchByEnums {
    ID("ID"),
    NAME("NAME"),
    ALL("ALL");

    @Getter
    private final String value;

    CollectionChannelSearchByEnums(String value) {
        this.value = value;
    }
}
