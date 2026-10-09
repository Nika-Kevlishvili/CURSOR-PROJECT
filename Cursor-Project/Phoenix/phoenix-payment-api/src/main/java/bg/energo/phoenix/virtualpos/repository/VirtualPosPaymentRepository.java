package bg.energo.phoenix.virtualpos.repository;

import bg.energo.phoenix.virtualpos.model.entity.VirtualPosPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VirtualPosPaymentRepository extends JpaRepository<VirtualPosPaymentEntity, Long> {
    Optional<VirtualPosPaymentEntity> findByTransactionId(String transactionId);
}


