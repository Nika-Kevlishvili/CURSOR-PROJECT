package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunCorrectionPods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunCorrectionPodsRepository extends JpaRepository<BillingRunCorrectionPods, Long> {

    List<BillingRunCorrectionPods> findAllByCorrectionRunIdAndRunId(Long correctionRunId, Long runId);
}
