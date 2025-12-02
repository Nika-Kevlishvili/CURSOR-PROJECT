package bg.energo.phoenix.repository.product.service.subObject;

import bg.energo.phoenix.model.entity.product.service.ServiceInterimAndAdvancePayment;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceInterimAndAdvancePaymentRepository extends JpaRepository<ServiceInterimAndAdvancePayment, Long> {
    @Query("""
            SELECT siap from ServiceInterimAndAdvancePayment siap
            where siap.interimAndAdvancePayment.id = :interimAdvancePaymentId and siap.serviceDetails.id = :serviceDetailsId and siap.status = 'ACTIVE'
            """)
    Optional<ServiceInterimAndAdvancePayment> findByInterimAndAdvancePaymentAndServiceDetailsId(Long interimAdvancePaymentId, Long serviceDetailsId);

    List<ServiceInterimAndAdvancePayment> findByServiceDetailsId(Long id);

    List<ServiceInterimAndAdvancePayment> findAllByServiceDetailsId(Long serviceDetailsId);

    List<ServiceInterimAndAdvancePayment> findByServiceDetailsIdAndStatusIn(Long serviceDetailId, List<ServiceSubobjectStatus> statuses);

}
