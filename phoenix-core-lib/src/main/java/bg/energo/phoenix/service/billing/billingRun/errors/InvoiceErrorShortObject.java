package bg.energo.phoenix.service.billing.billingRun.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceErrorShortObject {

    private String invoiceNumber;
    private String errorMessage;
}
