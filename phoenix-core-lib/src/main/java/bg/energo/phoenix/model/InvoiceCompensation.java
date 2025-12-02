package bg.energo.phoenix.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class InvoiceCompensation {

    private Long invoiceId;
    private Long customerId;
    private Long podId;
    private Long currencyId;
    private LocalDate compensationDocumentPeriod;
    private Long compensationReceiptId;
    private BigDecimal compAmount;
    private Integer compensationIndex;
    private Set<Long> compensationIds;

}
