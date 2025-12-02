package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import lombok.Data;

@Data
public class LatePaymentFineShortResponse {

    private Long id;
    private String latePaymentNumber;

    public LatePaymentFineShortResponse(LatePaymentFine latePaymentFine) {
        this.id = latePaymentFine.getId();
        this.latePaymentNumber = latePaymentFine.getLatePaymentNumber();
    }
}
