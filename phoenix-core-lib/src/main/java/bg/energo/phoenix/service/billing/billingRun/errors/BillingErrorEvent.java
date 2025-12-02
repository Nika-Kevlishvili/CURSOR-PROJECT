package bg.energo.phoenix.service.billing.billingRun.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BillingErrorEvent {
    private Long billingId;
    private List<InvoiceErrorShortObject> errorMessages;
    private BillingProtocol protocol;

}
