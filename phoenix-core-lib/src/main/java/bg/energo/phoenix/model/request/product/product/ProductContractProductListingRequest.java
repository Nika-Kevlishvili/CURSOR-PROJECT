package bg.energo.phoenix.model.request.product.product;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import lombok.Data;

@Data
@PromptSymbolReplacer
public class ProductContractProductListingRequest {

    private Long customerDetailId;
    private Long productContractId;
    private String prompt;
    private Integer page;
    private Integer size;
}
