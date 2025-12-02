package bg.energo.phoenix.model.request.product.price.priceComponent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlternateInvoiceRecipient {

    private String identifier;
    private Long customerId;
    private String customerName;
    private Long versionId;
    private LocalDateTime startDate;
}
