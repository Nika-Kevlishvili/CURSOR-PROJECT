package bg.energo.phoenix.model.request.activity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSystemActivityRequest extends BaseSystemActivityRequest {

    @NotNull(message = "activityId-Activity id should be specified;")
    private Long activityId;

    @NotNull(message = "subActivityId-Sub activity id should be specified;")
    private Long subActivityId;

    @NotNull(message = "objectId-Object id should be specified;")
    private Long objectId; // This is the ID of the domain object (e.g. customer ID, contract ID, etc.)

}
