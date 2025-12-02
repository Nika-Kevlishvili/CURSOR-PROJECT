package bg.energo.phoenix.model.request.product.termination.terminations;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditTerminationRequest extends BaseTerminationRequest{

    @NotNull(message = "id-Termination id is required;")
    private Long id;

}
