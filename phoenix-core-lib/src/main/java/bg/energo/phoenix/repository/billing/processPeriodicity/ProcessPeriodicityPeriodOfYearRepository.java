package bg.energo.phoenix.repository.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityPeriodOfYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessPeriodicityPeriodOfYearRepository extends JpaRepository<ProcessPeriodicityPeriodOfYear, Long> {
    List<ProcessPeriodicityPeriodOfYear> findAllByProcessPeriodicityIdAndStatus(Long processPeriodicityId, EntityStatus status);

}
