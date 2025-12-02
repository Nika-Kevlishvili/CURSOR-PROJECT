package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@PromptSymbolReplacer
public class ContractSaleListRequest {
    @NotNull(message = "page-Page size must not be null;")
    private int page;

    @NotNull(message = "size-Size must not be null;")
    private int size;

    @Size(min = 1, message = "prompt-Prompt should contain minimum 1 characters;")
    private String prompt;

    private String serviceName;

    private List<Long> segmentIds;
}
