package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.EasyPayPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EasyPayPaymentRepository extends JpaRepository<EasyPayPaymentEntity, Long> {
    Optional<EasyPayPaymentEntity> findByTransactionId(String transactionId);
}
