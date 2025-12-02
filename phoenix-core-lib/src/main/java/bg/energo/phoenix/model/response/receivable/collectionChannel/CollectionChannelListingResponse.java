package bg.energo.phoenix.model.response.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import lombok.Data;

@Data
public class CollectionChannelListingResponse {
    private Long id;
    private String name;
    private String collectionPartner;
    private CollectionChannelType type;
    private String currency;
    private EntityStatus status;

    public CollectionChannelListingResponse(CollectionChannelListingMiddleResponse collectionChannelListingMiddleResponse) {
        this.id = collectionChannelListingMiddleResponse.getId();
        this.name = collectionChannelListingMiddleResponse.getName();
        this.collectionPartner = collectionChannelListingMiddleResponse.getCollectionPartner();
        this.type = collectionChannelListingMiddleResponse.getType();
        this.currency = collectionChannelListingMiddleResponse.getCurrency();
        this.status = collectionChannelListingMiddleResponse.getStatus();
    }
}
