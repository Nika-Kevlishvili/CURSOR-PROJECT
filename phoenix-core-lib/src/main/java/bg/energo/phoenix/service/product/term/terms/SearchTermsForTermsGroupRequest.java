package bg.energo.phoenix.service.product.term.terms;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class SearchTermsForTermsGroupRequest {
    @NotNull(message = "page- Page shouldn't be null;")
    private Integer page;
    @NotNull(message = "size-Size shouldn't be null;")
    private Integer size;
    private String prompt;
}