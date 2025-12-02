package bg.energo.phoenix.repository.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineCommunications;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LatePaymentFineCommunicationsRepository extends JpaRepository<LatePaymentFineCommunications, Long> {

    List<LatePaymentFineCommunications> findByLatePaymentId(Long latePaymentFineId);

}
