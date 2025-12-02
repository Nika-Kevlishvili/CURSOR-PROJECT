package bg.energo.phoenix.model.request.product.term.terms;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchTermsForTermsGroupRequest {
    @NotNull(message = "page-shouldn't be null")
    Integer page;
    @NotNull(message = "size-shouldn't be null")
    Integer size;
    String prompt;
}
