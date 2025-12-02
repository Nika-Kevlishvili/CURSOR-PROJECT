package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@PromptSymbolReplacer
public class GoodsSearchRequest {
    private String prompt;

    @Min(value = 0, message = "page-Page must be greater then 0;")
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @Min(value = 1, message = "size-Size must be greater then 1;")
    @NotNull(message = "size-Size must not be null;")
    private Integer size;
}
