package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceFileExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceFileExportRepository extends JpaRepository<InvoiceFileExport,Long> {

    @Query("""
            select ife from InvoiceFileExport  ife
            where ife.billingRunId = :billingRunId
            and ife.status='ACTIVE'

            """)
    Optional<InvoiceFileExport> findInvoiceFIleByBillingRun(Long billingRunId);
}
