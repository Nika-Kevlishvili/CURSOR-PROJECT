package bg.energo.phoenix.repository.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityTimeIntervals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessPeriodicityTimeIntervalsRepository extends JpaRepository<ProcessPeriodicityTimeIntervals, Long> {
    List<ProcessPeriodicityTimeIntervals> findAllByProcessPeriodicityIdAndStatus(Long processPeriodicityId, EntityStatus status);

}
