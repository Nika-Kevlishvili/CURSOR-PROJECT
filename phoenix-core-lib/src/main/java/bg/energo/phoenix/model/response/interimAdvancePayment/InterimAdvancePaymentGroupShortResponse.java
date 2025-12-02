package bg.energo.phoenix.model.response.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import lombok.Data;

@Data
public class InterimAdvancePaymentGroupShortResponse {
    private Long id;
    private String name;

    public InterimAdvancePaymentGroupShortResponse(AdvancedPaymentGroupDetails advancedPaymentGroupDetails) {
        Long advancedPaymentGroupId = advancedPaymentGroupDetails.getAdvancedPaymentGroupId();
        this.id = advancedPaymentGroupId;
        this.name = "%s (%s)".formatted(advancedPaymentGroupDetails.getName(), advancedPaymentGroupId);
    }
}
