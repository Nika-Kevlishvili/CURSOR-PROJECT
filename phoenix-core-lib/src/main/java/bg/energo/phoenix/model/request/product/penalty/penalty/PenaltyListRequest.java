package bg.energo.phoenix.model.request.product.penalty.penalty;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PartyReceivingPenalty;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyApplicability;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltySearchField;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyTableColumn;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyAvailability;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.domain.Sort;

import java.util.Set;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class PenaltyListRequest {

    @NotNull(message = "page-Page size must not be null")
    private int page;

    @NotNull(message = "size-Size must not be null")
    private int size;

    @Size(min = 1,message = "prompt-Prompt should contain minimum 1 characters")
    private String prompt;

    private PenaltySearchField searchBy;

    private PenaltyTableColumn sortBy;

    private Sort.Direction sortDirection;

    private Set<PartyReceivingPenalty> penaltyReceivingParties;

    private Set<PenaltyApplicability> applicability;

    @NotNull(message = "availability-Availability you can not provide null value explicitly!")
    private PenaltyAvailability available = PenaltyAvailability.ALL;
}
