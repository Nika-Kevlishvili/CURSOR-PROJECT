package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellationDocument;
import bg.energo.phoenix.model.response.proxy.ProxyFileResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceCancellationDocumentRepository extends JpaRepository<InvoiceCancellationDocument, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.proxy.ProxyFileResponse(
            d.id,
            d.name
            )
            from Document d
            join InvoiceCancellationDocument icd on icd.documentId = d.id
            where icd.status = 'ACTIVE'
            and d.status = 'ACTIVE'
            and icd.cancelledInvoiceId = :id
            """)
    Optional<ProxyFileResponse> findByInvoiceCancellationId(Long id);
}
