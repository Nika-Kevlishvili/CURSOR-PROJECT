package bg.energo.phoenix.model.request.pod.pod;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PodCreateRequest extends PodBaseRequest {

    @NotBlank(message = "identifier-Identifier can not be blank;")
    @Size(min = 1, max = 33, message = "identifier-Identifier length should be between {min}:{max};")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "identifier-Allowed symbols in identifier are: A-Z a-z 0-9;")
    private String identifier;

    @NotNull(message = "gridOperatorId-Grid Operator can not be null;")
    @Min(value = 1, message = "gridOperatorId-Grid Operator ID min value should be 1;")
    protected Long gridOperatorId;

}
