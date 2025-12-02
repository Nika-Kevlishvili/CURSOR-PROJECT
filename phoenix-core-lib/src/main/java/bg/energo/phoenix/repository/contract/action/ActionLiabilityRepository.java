package bg.energo.phoenix.repository.contract.action;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.ActionLiability;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionLiabilityRepository extends JpaRepository<ActionLiability, Long> {

    boolean existsByActionIdAndStatus(Long actionId, EntityStatus status);

}
