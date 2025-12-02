package bg.energo.phoenix.model.request.task;

import bg.energo.phoenix.model.enums.task.ConnectedEntityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskConnectedEntity {
    @NotNull(message = "connectedEntities.id-Id must not be null;")
    private Long id;

    @NotNull(message = "connectedEntities.entityType-Entity Type must not be null;")
    private ConnectedEntityType entityType;
}
