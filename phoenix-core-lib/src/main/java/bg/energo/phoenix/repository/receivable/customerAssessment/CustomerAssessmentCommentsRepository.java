package bg.energo.phoenix.repository.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentComments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAssessmentCommentsRepository extends JpaRepository<CustomerAssessmentComments, Long> {

    @Query("""
            select cac from CustomerAssessmentComments cac
            where cac.customerAssessmentId = :id
            order by cac.createDate
            """)
    List<CustomerAssessmentComments> findAllByCustomerAssessmentId(@Param("id") Long id);
}
