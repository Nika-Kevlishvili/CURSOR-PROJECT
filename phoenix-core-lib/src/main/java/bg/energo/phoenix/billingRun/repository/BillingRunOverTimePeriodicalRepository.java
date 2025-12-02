package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunOverTimePeriodical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunOverTimePeriodicalRepository extends JpaRepository<BillingRunOverTimePeriodical,Long> {
    List<BillingRunOverTimePeriodical> findAllByRunContractId(Long runContractId);
}
