package bg.energo.phoenix.model.response.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import lombok.Data;

@Data
public class CollectionChannelShortResponse {
    private Long id;
    private String name;

    public CollectionChannelShortResponse(CollectionChannel collectionChannel) {
        this.id = collectionChannel.getId();
        this.name = collectionChannel.getName();
    }
}
