package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PromptSymbolReplacer
public class AvailableServiceRelatedEntityRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    @Size(min = 1, message = "prompt-Prompt must contain minimum 1 character;")
    private String prompt;

    private Long excludedId; // ID of the service which is being edited

    private Long excludedItemId; // ID of the service/product which is selected in dropdown

    private String excludedItemType;

}
