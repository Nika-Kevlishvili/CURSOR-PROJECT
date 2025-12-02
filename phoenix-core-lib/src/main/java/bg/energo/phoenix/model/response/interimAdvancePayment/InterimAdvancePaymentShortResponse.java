package bg.energo.phoenix.model.response.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import lombok.Data;

@Data
public class InterimAdvancePaymentShortResponse {
    private Long id;
    private String name;

    public InterimAdvancePaymentShortResponse(InterimAdvancePayment payment) {
        this.id = payment.getId();
        this.name = "%s (%s)".formatted(payment.getName(), payment.getId());
    }
}
