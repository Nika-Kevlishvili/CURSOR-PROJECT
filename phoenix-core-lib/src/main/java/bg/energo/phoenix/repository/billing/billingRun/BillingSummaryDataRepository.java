package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingSummaryDataRepository extends JpaRepository<BillingSummaryData, Long> {
    List<BillingSummaryData> findByBillingId(Long id);
}
