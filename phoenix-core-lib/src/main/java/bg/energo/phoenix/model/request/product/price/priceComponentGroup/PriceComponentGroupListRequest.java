package bg.energo.phoenix.model.request.product.price.priceComponentGroup;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupSearchField;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupTableColumn;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.domain.Sort;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class PriceComponentGroupListRequest {

    @NotNull(message = "page-Page size must not be null;")
    private int page;

    @NotNull(message = "size-Size must not be null;")
    private int size;

    @Size(min = 1,message = "prompt-Prompt should contain minimum 1 characters;")
    private String prompt;

    private PriceComponentGroupSearchField searchBy;

    private PriceComponentGroupTableColumn sortBy;

    private Boolean excludeOldVersions;

    private Boolean excludeFutureVersions;

    private Sort.Direction sortDirection;

}
