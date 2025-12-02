package bg.energo.phoenix.repository.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityIncompatibleProcesses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessPeriodicityIncompatibleProcessesRepository extends JpaRepository<ProcessPeriodicityIncompatibleProcesses, Long> {

    List<ProcessPeriodicityIncompatibleProcesses> findAllByProcessPeriodicityIdAndStatus(Long processPeriodicityId, EntityStatus status);

    List<ProcessPeriodicityIncompatibleProcesses> findByProcessPeriodicityIdAndIncompatibleBillingIdAndStatus(Long processPeriodicityId,Long billingRunId, EntityStatus entityStatus);
}
