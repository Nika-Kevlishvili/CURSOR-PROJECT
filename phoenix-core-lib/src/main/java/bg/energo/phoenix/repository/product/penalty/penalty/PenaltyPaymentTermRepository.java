package bg.energo.phoenix.repository.product.penalty.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyPaymentTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface PenaltyPaymentTermRepository extends JpaRepository<PenaltyPaymentTerm, Long> {

    Optional<PenaltyPaymentTerm> findByPenaltyIdAndStatus(Long penaltyId, EntityStatus status);




    Optional<PenaltyPaymentTerm> findByPenaltyIdAndStatusIn(Long penaltyId, List<EntityStatus> statuses);

}
