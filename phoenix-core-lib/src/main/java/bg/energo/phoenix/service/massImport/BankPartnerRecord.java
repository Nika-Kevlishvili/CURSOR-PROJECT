package bg.energo.phoenix.service.massImport;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankPartnerRecord {
    private String accountIdentification;
    private String currency;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String reference;
    private String invoicePrefix;
    private String invoiceNumber;
    private Long customerNumber;
    private Long collectionChannelId;
}
