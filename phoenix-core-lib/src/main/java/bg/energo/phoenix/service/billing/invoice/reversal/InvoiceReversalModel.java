package bg.energo.phoenix.service.billing.invoice.reversal;

import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceReversalModel {

    private Long invoiceId;
    private InvoiceDocumentType documentType;
    private InvoiceType invoiceType;
    private Long standardId;
    private String invoiceNumber;
}
