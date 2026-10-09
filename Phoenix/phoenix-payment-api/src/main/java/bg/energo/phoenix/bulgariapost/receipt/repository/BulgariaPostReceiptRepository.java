package bg.energo.phoenix.bulgariapost.receipt.repository;

import bg.energo.phoenix.bulgariapost.receipt.entities.BulgariaPostReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BulgariaPostReceiptRepository extends JpaRepository<BulgariaPostReceiptEntity, Long> {

    boolean existsByTid(String tid);

    Optional<BulgariaPostReceiptEntity> findByTid(String tid);
}

