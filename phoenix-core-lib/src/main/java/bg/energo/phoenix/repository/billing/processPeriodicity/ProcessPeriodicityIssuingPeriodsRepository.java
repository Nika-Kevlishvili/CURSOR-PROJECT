package bg.energo.phoenix.repository.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityIssuingPeriods;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessPeriodicityIssuingPeriodsRepository extends JpaRepository<ProcessPeriodicityIssuingPeriods, Long> {
    List<ProcessPeriodicityIssuingPeriods> findAllByProcessPeriodicityIdAndStatus(Long processPeriodicityId, EntityStatus status);
}
