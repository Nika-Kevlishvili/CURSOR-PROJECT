package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDocumentFile;
import bg.energo.phoenix.model.entity.documents.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDocumentFileRepository extends JpaRepository<InvoiceDocumentFile, Long> {

    @Query("""
            select doc from InvoiceDocumentFile idf
            join Document doc on doc.id=idf.documentId
            where doc.status='ACTIVE'
            and idf.invoiceId=:invoiceId
            """)
    List<Document> findAllByInvoiceId(Long invoiceId);

    List<InvoiceDocumentFile> findAllByInvoiceIdIn(List<Long> invoiceIds);
}
