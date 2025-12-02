package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, Long> {
    Optional<InvoiceDocument> findInvoiceDocumentByInvoiceId(Long invoiceId);

    List<InvoiceDocument> findAllByInvoiceIdIn(List<Long> invoiceIds);

    @Query(value = """
            select doc.id + 1
            from invoice.invoice_documents doc
            order by doc.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();
}