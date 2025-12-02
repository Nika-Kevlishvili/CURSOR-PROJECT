package bg.energo.phoenix.model.request.product.termination.terminations;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationAvailability;
import bg.energo.phoenix.model.enums.product.termination.terminations.filter.TerminationSearchFields;
import bg.energo.phoenix.model.enums.product.termination.terminations.filter.TerminationSortFields;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class TerminationListRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    @Size(min = 1, message = "prompt-Prompt must contain minimum 1 character;")
    private String prompt;

    private TerminationSearchFields terminationSearchFields;

    private List<Boolean> autoTermination;

    private Boolean noticeDue;

    private TerminationSortFields terminationSortFields;

    private Sort.Direction sortDirection;

    @NotNull(message = "availability-Availability you can not provide null value explicitly!")
    private TerminationAvailability availability = TerminationAvailability.ALL;

}
