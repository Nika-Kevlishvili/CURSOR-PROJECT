package bg.energo.phoenix.model.response.billing.billingRun;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceReversalBillingRunParametersResponse {
    private String listOfInvoices;
    private Long fileId;
    private String fileName;
}
