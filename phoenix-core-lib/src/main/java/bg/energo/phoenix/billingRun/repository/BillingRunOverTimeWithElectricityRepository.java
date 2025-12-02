package bg.energo.phoenix.billingRun.repository;

import bg.energo.phoenix.billingRun.model.entity.BillingRunOverTimeWithElectricity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRunOverTimeWithElectricityRepository extends JpaRepository<BillingRunOverTimeWithElectricity, Long> {
    List<BillingRunOverTimeWithElectricity> findAllByRunContractId(Long id);
}
