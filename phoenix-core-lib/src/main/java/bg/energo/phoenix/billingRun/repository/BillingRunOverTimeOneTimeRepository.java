package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunOverTimeOneTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunOverTimeOneTimeRepository extends JpaRepository<BillingRunOverTimeOneTime, Long> {
    List<BillingRunOverTimeOneTime> findAllByRunContractId(Long runContractId);
}
