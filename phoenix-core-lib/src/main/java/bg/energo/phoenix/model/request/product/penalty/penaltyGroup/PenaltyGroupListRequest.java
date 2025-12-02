package bg.energo.phoenix.model.request.product.penalty.penaltyGroup;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupSearchField;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroupTableColumn;
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
public class PenaltyGroupListRequest {

    @NotNull(message = "page-Page size must not be null")
    private int page;

    @NotNull(message = "size-Size must not be null")
    private int size;

    @Size(min = 1,message = "prompt-Prompt should contain minimum 1 characters")
    private String prompt;

    private PenaltyGroupSearchField searchBy;

    private PenaltyGroupTableColumn sortBy;

    private Sort.Direction sortDirection;

    private boolean excludeOldVersions;

    private boolean excludeFutureVersions;

}