package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunInterimData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunInterimDataRepository extends JpaRepository<BillingRunInterimData,Long> {
    List<BillingRunInterimData> findAllByRunIdAndIsValidForGenerationAndStatus(Long runId, Boolean isValidForGeneration, String status);
}
