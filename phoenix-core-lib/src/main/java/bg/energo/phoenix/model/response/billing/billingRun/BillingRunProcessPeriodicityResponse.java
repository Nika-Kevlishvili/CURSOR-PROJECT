package bg.energo.phoenix.model.response.billing.billingRun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingRunProcessPeriodicityResponse {
    private Long id;
    private String name;
}
