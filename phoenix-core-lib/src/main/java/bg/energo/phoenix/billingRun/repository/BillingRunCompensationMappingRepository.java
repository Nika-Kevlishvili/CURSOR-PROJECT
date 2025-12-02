package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunCompensationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BillingRunCompensationMappingRepository  extends JpaRepository<BillingRunCompensationMapping, Long> {

    List<BillingRunCompensationMapping> findAllByBgInvoiceSlotIdAndRunId(Long bgInvoiceSlotId, Long runId);
}
