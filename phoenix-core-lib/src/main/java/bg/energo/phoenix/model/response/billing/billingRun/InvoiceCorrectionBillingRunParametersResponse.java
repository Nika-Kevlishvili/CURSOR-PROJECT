package bg.energo.phoenix.model.response.billing.billingRun;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceCorrectionBillingRunParametersResponse {

    private String listOfInvoices;
    private Boolean priceChanges;
    private Boolean volumeChange;
    private Long fileId;
    private String fileName;

}
