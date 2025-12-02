package bg.energo.phoenix.repository.product.penalty.penalty;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penalty.PenaltyActionTypes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyActionTypesRepository extends JpaRepository<PenaltyActionTypes, Long> {
    List<PenaltyActionTypes> findAllByPenaltyIdAndStatus(Long penaltyId, EntityStatus entityStatus);
}
