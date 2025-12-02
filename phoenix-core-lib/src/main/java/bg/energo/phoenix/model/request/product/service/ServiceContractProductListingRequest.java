package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import lombok.Data;

@Data
@PromptSymbolReplacer
public class ServiceContractProductListingRequest {
    private Long customerDetailId;
    private Long serviceContractId;
    private String prompt;
    private Integer page;
    private Integer size;
}
