package bg.energo.phoenix.model.response.billing.invoice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceCommunicationResponse {
    private Long id;
    private String name;
    private Boolean fromBillingGroup;

    public InvoiceCommunicationResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
