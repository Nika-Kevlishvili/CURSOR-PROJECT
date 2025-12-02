package bg.energo.phoenix.repository.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentAddCondition;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAssessmentAddConditionRepository extends JpaRepository<CustomerAssessmentAddCondition, Long> {

    @Query(
            """
                    select new bg.energo.phoenix.model.response.shared.ShortResponse(ac.id,ac.name)
                      from AdditionalCondition ac
                      join CustomerAssessmentAddCondition cat on cat.additionalConditionId= ac.id
                      where cat.customerAssessmentId = :id
                      and ac.status = 'ACTIVE'
                      and cat.status ='ACTIVE'
                               """
    )
    List<ShortResponse> findAllByCustomerAssessmentId(Long id);

    Optional<List<CustomerAssessmentAddCondition>> findAllByCustomerAssessmentIdAndStatus(Long customerAssessmentId, EntityStatus status);
}
