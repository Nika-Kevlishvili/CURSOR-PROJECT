package bg.energo.phoenix.model.response.billing.billingRun.manualInvoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractOrderShortResponse {
    private Long id;
    private String number;
}
