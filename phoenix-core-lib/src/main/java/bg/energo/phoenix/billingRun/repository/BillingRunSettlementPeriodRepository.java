package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunSettlementPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunSettlementPeriodRepository extends JpaRepository<BillingRunSettlementPeriod, Long> {
    List<BillingRunSettlementPeriod> findAllByRunContractId(Long runContractId);
}
