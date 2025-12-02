package bg.energo.phoenix.repository.nomenclature.billing;

import bg.energo.phoenix.model.entity.nomenclature.billing.RiskAssessment;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    @Query(
            """
                    select ra from RiskAssessment as ra
                        where ra.id<> :currentId
                        and (ra.orderingId >= :start and ra.orderingId <= :end)
                    """
    )
    List<RiskAssessment> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select ra from RiskAssessment as ra
                        where ra.orderingId is not null
                        order by ra.name
                    """
    )
    List<RiskAssessment> orderByName();

    @Query(
            """
                    select count(1) from RiskAssessment ra
                        where lower(ra.name) = lower(:name)
                        and ra.status in :statuses
                    """
    )
    Long countRiskAssessmentByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<RiskAssessment> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from RiskAssessment s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
                    select sa from RiskAssessment as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and sa.status in (:statuses)
                        and :excludedItemId is null or sa.id <> :excludedItemId
                        order by sa.defaultSelection desc, sa.orderingId asc
                    """
    )
    Page<RiskAssessment> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select max(ra.orderingId) from RiskAssessment ra
                    """
    )
    Long findLastOrderingId();

    @Query(
            value = """
                    select sa from RiskAssessment as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and ((sa.status in (:statuses))
                        and (:excludedItemId is null or sa.id <> :excludedItemId)
                        or (sa.id in (:includedItemIds)))
                        order by case when sa.id in (:includedItemIds) then 1 else 2 end,
                        sa.defaultSelection desc, sa.orderingId asc
                    """, countQuery = """
            select count(1) from RiskAssessment as sa
                where (:prompt is null or (
                    lower(sa.name) like :prompt
                ))
                and ((sa.status in (:statuses))
                and (:excludedItemId is null or sa.id <> :excludedItemId)
                or (sa.id in (:includedItemIds)))
                """
    )
    Page<RiskAssessment> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );


}
