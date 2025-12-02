package bg.energo.phoenix.model.request.product.termination.terminationGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class BaseTerminationGroupRequest {

    @NotBlank(message = "name-Name must not be blank;")
    @Size(min = 1, max = 1024, message = "name-Name length should be between {min} and {max} symbols range;")
    private String name;

}
