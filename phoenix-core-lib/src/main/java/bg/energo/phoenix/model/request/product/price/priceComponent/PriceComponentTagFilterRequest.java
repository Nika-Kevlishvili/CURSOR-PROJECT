package bg.energo.phoenix.model.request.product.price.priceComponent;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class PriceComponentTagFilterRequest {
    @NotNull(message = "page-shouldn't be null")
    private Integer page;
    @NotNull(message = "size-shouldn't be null")
    private Integer size;
    private String prompt;
    private Sort.Direction columnDirection;
}
