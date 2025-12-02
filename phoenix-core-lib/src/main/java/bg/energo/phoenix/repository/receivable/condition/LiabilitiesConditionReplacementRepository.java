package bg.energo.phoenix.repository.receivable.condition;

import bg.energo.phoenix.model.entity.receivable.condition.LiabilitiesConditionReplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiabilitiesConditionReplacementRepository extends JpaRepository<LiabilitiesConditionReplacement, Long> {
}
