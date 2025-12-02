package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingInvoiceCorrectionInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingInvoiceCorrectionInvoiceRepository extends JpaRepository<BillingInvoiceCorrectionInvoice, Long> {

    void deleteAllByBillingId(Long billingId);
}
