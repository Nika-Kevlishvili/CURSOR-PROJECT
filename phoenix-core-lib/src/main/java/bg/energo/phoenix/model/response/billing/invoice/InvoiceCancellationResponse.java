package bg.energo.phoenix.model.response.billing.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InvoiceCancellationResponse {

    private Long invoiceCancellationId;
    private Long processId;

}
