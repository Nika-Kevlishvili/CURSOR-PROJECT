package bg.energo.phoenix.model.request.product.termination.terminations;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class AvailableTerminationSearchRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    @Size(min = 1, message = "prompt-Prompt must contain minimum 1 character;")
    private String prompt;

}
