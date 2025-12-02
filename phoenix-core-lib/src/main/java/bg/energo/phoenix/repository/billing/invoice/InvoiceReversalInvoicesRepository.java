package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceReversalInvoice;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModelExtended;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceReversalInvoicesRepository extends JpaRepository<InvoiceReversalInvoice, Long> {


    @Query("""
             select new bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModelExtended(iri.invoiceId,inv.invoiceDocumentType,inv.invoiceType,inv.standardInvoiceId,inv.invoiceNumber,iri) 
             from InvoiceReversalInvoice iri 
             join Invoice inv on iri.invoiceId=inv.id
            where iri.reversalId=:reversalId
            and iri.reversalStatus='NOT_STARTED'
                       
             """)
    List<InvoiceReversalModelExtended> findReversalModels(Long reversalId);

}
