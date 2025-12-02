package bg.energo.phoenix.model.request.receivable.collectionChannel.listing;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CollectionChannelType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
public class CollectionChannelListingRequest {

    @Size(min = 1, message = "prompt-Prompt length must be 1 or more")
    private String prompt;

    private List<CollectionChannelType> collectionChannelType;

    private CollectionChannelListColumns sortBy;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private CollectionChannelSearchByEnums searchBy;

    private List<Long> collectionPartnerId;

    private List<Long> currencyId;

    private Sort.Direction direction;

    private List<EntityStatus> statuses;

}
