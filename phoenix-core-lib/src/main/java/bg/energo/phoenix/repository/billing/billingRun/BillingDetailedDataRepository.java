package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingDetailedDataRepository extends JpaRepository<BillingDetailedData, Long> {
    List<BillingDetailedData> findByBillingId(Long id);

}
