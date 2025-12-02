package bg.energo.phoenix.model.response.billing.billingRun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillingErrorDataResponse {

    private String invoiceNumber;
    private String message;
    private Long billingRunId;;

}
