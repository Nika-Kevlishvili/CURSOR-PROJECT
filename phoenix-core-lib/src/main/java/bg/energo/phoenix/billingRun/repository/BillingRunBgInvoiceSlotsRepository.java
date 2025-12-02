package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunBgInvoiceSlots;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunBgInvoiceSlotsRepository extends JpaRepository<BillingRunBgInvoiceSlots, Long> {

    BillingRunBgInvoiceSlots findByBgInvoiceSlotId(Long bgInvoiceSlotId);

    BillingRunBgInvoiceSlots findByBgInvoiceSlotIdAndRunIdAndContractId(Long slotId, Long runId, Long contractId);

    @Query(value = """
                select bg.bgInvoiceSlotId from BillingRunBgInvoiceSlots bg where bg.runId = :runId and bg.status = 'ERROR'
                """)
    List<Long> findAllFailedSlots(Long runId);

}
