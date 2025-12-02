package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunCorrectionInvoiceBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunCorrectionInvoiceBaseRepository extends JpaRepository<BillingRunCorrectionInvoiceBase, Long> {

    List<BillingRunCorrectionInvoiceBase> findByRunIdAndReverseStatus(Long runId, String reverseStatus);
}
