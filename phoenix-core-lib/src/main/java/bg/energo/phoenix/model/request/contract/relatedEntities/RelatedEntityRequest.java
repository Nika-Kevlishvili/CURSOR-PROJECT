package bg.energo.phoenix.model.request.contract.relatedEntities;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RelatedEntityRequest {

    private Long id;

    private Long entityId;

    private RelatedEntityType entityType;

    @NotNull(message = "basicParameters.relatedEntities.relatedEntityId-Related entity ID is required;")
    private Long relatedEntityId;

    @NotNull(message = "basicParameters.relatedEntities.relatedEntityType-Related entity type is required;")
    private RelatedEntityType relatedEntityType;

}
