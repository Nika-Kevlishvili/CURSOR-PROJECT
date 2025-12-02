package bg.energo.phoenix.model.request.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentAvailability;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.ValueType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.filter.InterimAdvancePaymentSearchField;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.filter.InterimAdvancePaymentSortField;
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
@NoArgsConstructor
@AllArgsConstructor
@PromptSymbolReplacer
public class InterimAdvancePaymentListingRequest {

    @NotNull(message = "page-shouldn't be null")
    private Integer page;
    @NotNull(message = "size-shouldn't be null")
    private Integer size;
    @Size(min = 1, message = "prompt-Prompt must contain minimum 1 character;")
    private String prompt;
    private InterimAdvancePaymentSearchField promptBy;
    private List<ValueType> valueType;
    private DeductionFrom deductionFrom;
    private InterimAdvancePaymentSortField sortBy;
    private Sort.Direction sortDirection;
    @NotNull(message = "availability-Availability you can not provide null value explicitly!")
    private InterimAdvancePaymentAvailability availability = InterimAdvancePaymentAvailability.ALL;

}
