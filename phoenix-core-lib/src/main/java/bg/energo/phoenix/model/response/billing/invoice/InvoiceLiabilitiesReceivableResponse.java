package bg.energo.phoenix.model.response.billing.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceLiabilitiesReceivableResponse {
    private Long id;
    private String name;
    private String type;
}
