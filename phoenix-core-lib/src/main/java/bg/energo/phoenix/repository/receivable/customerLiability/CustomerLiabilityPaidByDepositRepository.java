package bg.energo.phoenix.repository.receivable.customerLiability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerLiabilityPaidByDepositRepository extends JpaRepository<CustomerLiabilityPaidByDeposit, Long> {
    List<CustomerLiabilityPaidByDeposit> findByCustomerLiabilityIdAndStatus(Long customerLiabilityId, EntityStatus status);

    boolean existsByCustomerDepositIdAndStatus(Long id,EntityStatus status);
}
