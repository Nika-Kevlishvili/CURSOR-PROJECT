package bg.energo.phoenix.model.response.billing.billingRun;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BillingRunFilterByResponse {

    private Long id;

    private String billingNumber;

}
