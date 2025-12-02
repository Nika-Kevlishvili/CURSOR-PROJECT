package bg.energo.phoenix.model.request.product.term.termsGroup;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermsGroupListColumns;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermsGroupParameterFilterField;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class TermsGroupListRequest {

    @NotNull(message = "page-Page shouldn't be null;")
    private Integer page;

    @NotNull(message = "size-Size shouldn't be null;")
    private Integer size;

    private String prompt;

    private TermsGroupParameterFilterField termsGroupParameterFilterField;

    private TermsGroupListColumns termsGroupListColumns;

    private Sort.Direction columnDirection;

    private Boolean excludeOldVersions;

    private Boolean excludeFutureVersions;

}
