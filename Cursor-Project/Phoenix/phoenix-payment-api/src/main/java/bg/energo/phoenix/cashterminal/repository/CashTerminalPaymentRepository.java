package bg.energo.phoenix.cashterminal.repository;

import bg.energo.phoenix.cashterminal.model.entity.CashTerminalPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashTerminalPaymentRepository extends JpaRepository<CashTerminalPaymentEntity, Long> {
    Optional<CashTerminalPaymentEntity> findByTransactionId(String transactionId);
}

