package bg.energo.phoenix.repository.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositPaymentDeadlineAfterWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface DepositPaymentDeadlineAfterWithdrawalRepository extends JpaRepository<DepositPaymentDeadlineAfterWithdrawal, Long> {
    Optional<DepositPaymentDeadlineAfterWithdrawal> findByDepositIdAndStatusIn(Long depositId, List<EntityStatus> statuses);
}
