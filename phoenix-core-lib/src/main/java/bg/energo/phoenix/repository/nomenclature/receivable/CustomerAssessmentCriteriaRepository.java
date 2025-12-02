package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentCriteria;
import bg.energo.phoenix.model.enums.nomenclature.CustomerAssessmentCriteriaType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAssessmentCriteriaRepository extends JpaRepository<CustomerAssessmentCriteria, Long> {

    Optional<CustomerAssessmentCriteria> findByIdAndStatus(Long id, NomenclatureItemStatus status);

    @Query(
            """
                    select cp from CustomerAssessmentCriteria as cp
                        where cp.id<> :currentId
                        and (cp.orderingId >= :start and cp.orderingId <= :end)
                    """
    )
    List<CustomerAssessmentCriteria> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select cp from CustomerAssessmentCriteria as cp
                        where cp.orderingId is not null
                        order by cp.name
                    """
    )
    List<CustomerAssessmentCriteria> orderByName();

    Optional<CustomerAssessmentCriteria> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from CustomerAssessmentCriteria s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
                    select sa from CustomerAssessmentCriteria as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and sa.status in (:statuses)
                        and :excludedItemId is null or sa.id <> :excludedItemId
                        order by sa.orderingId asc
                    """
    )
    Page<CustomerAssessmentCriteria> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select max(ra.orderingId) from CustomerAssessmentCriteria ra
                    """
    )
    Long findLastOrderingId();


    @Query(
            """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(p.id, p.name, p.orderingId, p.defaultSelection, p.status)
                        from CustomerAssessmentCriteria as p
                        where (:prompt is null or lower(p.name) like :prompt)
                        and (p.status in (:statuses))
                        order by p.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<CustomerAssessmentCriteria> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    List<CustomerAssessmentCriteria> findAllByStatusAndCriteriaTypeIn(NomenclatureItemStatus status, List<CustomerAssessmentCriteriaType> criteriaType);
}
