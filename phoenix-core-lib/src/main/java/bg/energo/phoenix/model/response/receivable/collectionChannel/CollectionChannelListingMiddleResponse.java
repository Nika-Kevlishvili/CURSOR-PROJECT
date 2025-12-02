package bg.energo.phoenix.model.response.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;

public interface CollectionChannelListingMiddleResponse {
    Long getId();

    String getName();

    String getCollectionPartner();

    CollectionChannelType getType();

    String getCurrency();

    EntityStatus getStatus();
}
