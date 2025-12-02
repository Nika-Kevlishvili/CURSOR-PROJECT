package bg.energo.phoenix.service.massImport;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentPartnerRecord {
    private Long customerNumber;
    private String customerIdentifier;
    private String billingGroupNumber;
    private String invoicePrefix;
    private String invoiceNumber;
    private BigDecimal amount;
    private Long collectionChannelId;
}
