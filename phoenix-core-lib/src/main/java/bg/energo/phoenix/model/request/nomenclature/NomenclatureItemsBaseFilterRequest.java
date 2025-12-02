package bg.energo.phoenix.model.request.nomenclature;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class NomenclatureItemsBaseFilterRequest {

    @NotNull(message = "statuses-statuses should not be null")
    @NotEmpty(message = "statuses-statuses should not be empty")
    private List<NomenclatureItemStatus> statuses;

    @Size(min = 1, max = 512, message = "prompt-Prompt does not match the allowed length")
    private String prompt;

    @NotNull(message = "page-page should not be null")
    private Integer page;

    @NotNull(message = "size-size should not be null")
    private Integer size;

    private Long excludedItemId;

    private List<Long> includedItemIds;

}
