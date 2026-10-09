package bg.energo.phoenix.bulgariapost.pay.repository;

import bg.energo.phoenix.bulgariapost.pay.entities.BulgariaPostPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulgariaPostPaymentRepository extends JpaRepository<BulgariaPostPaymentEntity, Long> {

    boolean existsByTid(String tid);
}
