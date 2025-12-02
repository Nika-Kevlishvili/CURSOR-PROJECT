package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunSvInvoiceSlots;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunSvInvoiceSlotsRepository extends JpaRepository<BillingRunSvInvoiceSlots,Long> {
    BillingRunSvInvoiceSlots findBySvInvoiceSlotIdAndRunIdAndContractId(Long slotId, Long runId, Long contractId);

    @Query(value = """
                select bg.svInvoiceSlotId from BillingRunSvInvoiceSlots bg where bg.runId = :runId and bg.status = 'ERROR'
                """)
    List<Long> findAllFailedSlots(Long runId);
}
