package bg.energo.phoenix.service.billing.invoice.reversal;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceReversalInvoice;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceReversalModelExtended extends InvoiceReversalModel {

    private InvoiceReversalInvoice invoiceReversalInvoice;


    public InvoiceReversalModelExtended(Long invoiceId, InvoiceDocumentType documentType, InvoiceType invoiceType, Long standardId,String invoiceNumber, InvoiceReversalInvoice invoiceReversalInvoice) {
        super(invoiceId, documentType, invoiceType, standardId,invoiceNumber);
        this.invoiceReversalInvoice = invoiceReversalInvoice;
    }

}
