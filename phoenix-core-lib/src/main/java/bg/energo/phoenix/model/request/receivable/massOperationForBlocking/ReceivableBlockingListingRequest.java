package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.BlockingSelection;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class ReceivableBlockingListingRequest {
    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private ReceivableBlockingListingSearchField searchField;

    private BlockingSelection blockedForPayment;

    private BlockingSelection blockedForLetters;

    private BlockingSelection blockedForCalculations;

    private BlockingSelection blockedForLiabilities;

    private BlockingSelection blockedForTermination;

    private Sort.Direction direction;

    private ReceivableBlockingListingColumn column;

    private List<EntityStatus> statuses;
}
