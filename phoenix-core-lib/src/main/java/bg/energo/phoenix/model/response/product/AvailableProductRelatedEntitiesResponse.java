package bg.energo.phoenix.model.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class AvailableProductRelatedEntitiesResponse {
    private long id;
    private String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RelatedEntityType type;

    public AvailableProductRelatedEntitiesResponse(Long id, String productDetailsName) {
        this.id = id;
        this.name = getFormatted(id, productDetailsName);
    }

    public AvailableProductRelatedEntitiesResponse(Long id, String productDetailsName, RelatedEntityType type) {
        this.id = id;
        this.name = getFormatted(id, productDetailsName);
        this.type = type;
    }

    private String getFormatted(Long id, String productDetailsName) {
        return "%s (%s)".formatted(productDetailsName, id);
    }
}
