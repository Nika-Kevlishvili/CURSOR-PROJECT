package bg.energo.phoenix.model.request.product.term.termsGroup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TermsGroupCreateRequest {

    @NotNull(message = "name-length should not be null")
    @Size(min = 1, max = 1024, message = "name-length should be 1-1024 characters")
    private String name;
    @NotNull(message = "termsId-must not be null;")
    private Long termsId;

}
