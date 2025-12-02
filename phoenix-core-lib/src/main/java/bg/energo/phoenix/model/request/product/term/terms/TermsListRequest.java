package bg.energo.phoenix.model.request.product.term.terms;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.term.terms.TermsAvailability;
import bg.energo.phoenix.model.enums.product.term.terms.filter.TermsListColumns;
import bg.energo.phoenix.model.enums.product.term.terms.filter.TermsParameterFilterField;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class TermsListRequest {

    @NotNull(message = "page-Page shouldn't be null;")
    private Integer page;

    @NotNull(message = "size-Size shouldn't be null;")
    private Integer size;

    private String prompt;

    private TermsParameterFilterField termsParameterFilterField;

    private TermsListColumns termsListColumns;

    private Sort.Direction columnDirection;

    @NotNull(message = "availability-availability you can not provide null value explicitly!")
    private TermsAvailability availability = TermsAvailability.ALL;

}
