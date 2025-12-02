package bg.energo.phoenix.model.request.product.price.priceParameter;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceListColumns;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterFilterField;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterFilterPriceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class PriceParameterFilterRequest {
    @NotNull(message = "page-shouldn't be null")
    Integer page;
    @NotNull(message = "size-shouldn't be null")
    Integer size;
    String prompt;
    PriceParameterFilterField filterField;
    List<PriceParameterFilterPriceType> priceParameterTypes;
    PriceListColumns priceListColumns;
    Sort.Direction columnDirection;
    boolean excludeOldVersions;
}
