package bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupFilterField;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentListColumns;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * <h1>AdvancedPaymentGroupListRequest</h1>
 * {@link #page} pagination page
 * {@link #size} pagination size
 * {@link #prompt} prompt string
 * {@link #promptBy} determines where to search {@link #prompt} value
 * {@link #sortBy} determines witch value to sort
 * {@link #sortDirection} sort direction ASC or DESC
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@PromptSymbolReplacer
public class AdvancedPaymentGroupListRequest {
    @NotNull(message = "page-shouldn't be null;")
    Integer page;
    @NotNull(message = "size-shouldn't be null;")
    Integer size;
    String prompt;
    AdvancedPaymentGroupFilterField promptBy;
    AdvancedPaymentListColumns sortBy;
    Sort.Direction sortDirection;
    boolean excludeOldVersions;
    boolean excludeFutureVersions;
}
