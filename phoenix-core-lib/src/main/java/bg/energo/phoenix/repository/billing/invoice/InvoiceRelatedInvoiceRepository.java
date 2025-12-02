package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceRelatedInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRelatedInvoiceRepository extends JpaRepository<InvoiceRelatedInvoice, Long> {
}