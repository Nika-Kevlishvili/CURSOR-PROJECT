package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.response.product.RelatedEntityType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class AvailableServiceRelatedEntitiyResponse {

    private long id;
    private String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RelatedEntityType type;

    public AvailableServiceRelatedEntitiyResponse(Long id, String productDetailsName) {
        this.id = id;
        this.name = getFormattedName(id, productDetailsName);
    }

    public AvailableServiceRelatedEntitiyResponse(Long id, String productDetailsName, RelatedEntityType type) {
        this.id = id;
        this.name = getFormattedName(id, productDetailsName);
        this.type = type;
    }

    private String getFormattedName(Long id, String relatedEntityName) {
        return "%s (%s)".formatted(relatedEntityName, id);
    }

}
