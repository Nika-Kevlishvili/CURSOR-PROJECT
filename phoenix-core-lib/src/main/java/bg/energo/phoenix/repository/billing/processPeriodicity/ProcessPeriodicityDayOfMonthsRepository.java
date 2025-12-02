package bg.energo.phoenix.repository.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityDayOfMonths;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessPeriodicityDayOfMonthsRepository extends JpaRepository<ProcessPeriodicityDayOfMonths, Long> {
    List<ProcessPeriodicityDayOfMonths> findAllByProcessPeriodicityIdAndStatus(Long processPeriodicityId, EntityStatus status);

}
