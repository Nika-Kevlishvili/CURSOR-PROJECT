package bg.energo.phoenix.systech.pay.repository;

import bg.energo.phoenix.systech.pay.entities.SystechPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystechPaymentRepository extends JpaRepository<SystechPaymentEntity, Long> {

    boolean existsByTid(String tid);
}
