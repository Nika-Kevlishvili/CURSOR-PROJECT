package bg.energo.phoenix.model.request.product.penalty.penaltyGroup;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PenaltyGroupCreateRequest {

    @NotBlank(message = "name-Name should not be blank")
    private String name;

    private List<Long> penalties;

}
