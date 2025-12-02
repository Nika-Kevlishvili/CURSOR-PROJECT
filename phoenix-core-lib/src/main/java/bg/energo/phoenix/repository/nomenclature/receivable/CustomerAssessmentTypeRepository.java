package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.nomenclature.receivable.AdditionalConditionShortResponse;
import bg.energo.phoenix.model.response.nomenclature.receivable.CustomerAssessmentTypeMiddleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAssessmentTypeRepository extends JpaRepository<CustomerAssessmentType, Long> {

    Optional<CustomerAssessmentType> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("select max(c.orderingId) from CustomerAssessmentType c")
    Long findLastOrderingId();

    @Query(
            value = """
                    select distinct on (s.name)
                             s.name, s.id
                             from nomenclature.customer_assessment_types s
                             where s.status = 'ACTIVE'
                             order by s.name
                              """
            , nativeQuery = true
    )
    List<CustomerAssessmentTypeMiddleResponse> findAllTypes();

    @Query(
            """
                    select new bg.energo.phoenix.model.response.nomenclature.receivable.AdditionalConditionShortResponse(ac.id,ac.name)
                      from AdditionalCondition ac
                      join CustomerAssessmentType cat on cat.id= ac.customerAssessmentTypeId
                      where cat.id = :id
                      and ac.status = 'ACTIVE'
                      and cat.status ='ACTIVE'
                               """
    )
    List<AdditionalConditionShortResponse> findAllByTypeId(Long id);

}
