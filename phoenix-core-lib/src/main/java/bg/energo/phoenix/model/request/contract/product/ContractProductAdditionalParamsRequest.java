package bg.energo.phoenix.model.request.contract.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ContractProductAdditionalParamsRequest {

    @NotNull(message = "id-id is required;")
    private Long id;

    @NotNull(message = "value-value required;")
    @Length(min = 1, max = 1024, message = "value-value length should be between {min} and {max};")
    private String value;

}
