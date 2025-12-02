package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingProcessPeriodicity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingProcessPeriodicityRepository extends JpaRepository<BillingProcessPeriodicity, Long> {

    List<BillingProcessPeriodicity> findByBillingIdAndStatus(Long id, EntityStatus status);

    Optional<BillingProcessPeriodicity> findByBillingIdAndProcessPeriodicityIdAndStatusIn(Long billingId, Long processPeriodicityId, List<EntityStatus> statues);

    List<BillingProcessPeriodicity> findByProcessPeriodicityIdNotInAndStatusIn(List<Long> processPeriodicityIds, List<EntityStatus> statuses);

    List<BillingProcessPeriodicity> findAllByBillingIdAndProcessPeriodicityIdAndStatus(Long id, Long processPeriodicityId, EntityStatus status);
}
