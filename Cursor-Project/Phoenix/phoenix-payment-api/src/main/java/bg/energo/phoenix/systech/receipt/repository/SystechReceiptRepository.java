package bg.energo.phoenix.systech.receipt.repository;

import bg.energo.phoenix.systech.receipt.entities.SystechReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystechReceiptRepository extends JpaRepository<SystechReceiptEntity, Long> {

    boolean existsByTid(String tid);

    Optional<SystechReceiptEntity> findByTid(String tid);
}

