package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.AdditionalCondition;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
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
public interface AdditionalConditionRepository extends JpaRepository<AdditionalCondition, Long> {


    @Query(
            """
                    select cp from AdditionalCondition as cp
                        where cp.id<> :currentId
                        and (cp.orderingId >= :start and cp.orderingId <= :end)
                    """
    )
    List<AdditionalCondition> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select cp from AdditionalCondition as cp
                        where cp.orderingId is not null
                        order by cp.name
                    """
    )
    List<AdditionalCondition> orderByName();

    @Query(
            """
                    select count(1) from AdditionalCondition cp
                        where lower(cp.name) = lower(:name)
                        and cp.status in :statuses
                    """
    )
    Long countAdditionalConditionByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<AdditionalCondition> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from AdditionalCondition s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
                    select sa from AdditionalCondition as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and sa.status in (:statuses)
                        and :excludedItemId is null or sa.id <> :excludedItemId
                        order by sa.orderingId asc
                    """
    )
    Page<AdditionalCondition> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select max(ra.orderingId) from AdditionalCondition ra
                    """
    )
    Long findLastOrderingId();

    @Query(
            value = """
                    select sa from AdditionalCondition as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and ((sa.status in (:statuses))
                        and (:excludedItemId is null or sa.id <> :excludedItemId)
                        or (sa.id in (:includedItemIds)))
                        order by case when sa.id in (:includedItemIds) then 1 else 2 end,
                        sa.defaultSelection desc, sa.orderingId asc
                    """, countQuery = """
            select count(1) from AdditionalCondition as sa
                where (:prompt is null or (
                    lower(sa.name) like :prompt
                ))
                and ((sa.status in (:statuses))
                and (:excludedItemId is null or sa.id <> :excludedItemId)
                or (sa.id in (:includedItemIds)))
                """
    )
    Page<AdditionalCondition> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    Optional<AdditionalCondition> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);
    @Query("""
            select count(a.id) > 0
            from CustomerAssessment a
            join CustomerAssessmentAddCondition ac
                on ac.customerAssessmentId = a.id
            where a.status = 'ACTIVE'
            and ac.status = 'ACTIVE'
            and ac.additionalConditionId = :id
            """)
    boolean isConnectedToCustomerAssessment(@Param("id") Long id);
}
