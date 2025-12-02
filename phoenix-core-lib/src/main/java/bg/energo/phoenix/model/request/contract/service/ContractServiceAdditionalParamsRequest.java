package bg.energo.phoenix.model.request.contract.service;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractServiceAdditionalParamsRequest {
    @NotNull(message = "id-id is required;")
    private Long id;

    @NotNull(message = "value-value required;")
    @Length(min = 1, max = 1024, message = "value-value length should be between {min} and {max};")
    private String value;
}
