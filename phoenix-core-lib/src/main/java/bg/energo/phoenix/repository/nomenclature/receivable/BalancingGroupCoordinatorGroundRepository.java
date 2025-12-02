package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.BalancingGroupCoordinatorGround;
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
public interface BalancingGroupCoordinatorGroundRepository extends JpaRepository<BalancingGroupCoordinatorGround, Long> {

    @Query(
            """
                    select cp from BalancingGroupCoordinatorGround as cp
                        where cp.id<> :currentId
                        and (cp.orderingId >= :start and cp.orderingId <= :end)
                    """
    )
    List<BalancingGroupCoordinatorGround> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select cp from BalancingGroupCoordinatorGround as cp
                        where cp.orderingId is not null
                        order by cp.name
                    """
    )
    List<BalancingGroupCoordinatorGround> orderByName();

    @Query(
            """
                    select count(1) from BalancingGroupCoordinatorGround cp
                        where lower(cp.name) = lower(:name)
                        and cp.status in :statuses
                    """
    )
    Long countBalancingGroupCoordinatorGroundByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<BalancingGroupCoordinatorGround> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from BalancingGroupCoordinatorGround s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
                    select sa from BalancingGroupCoordinatorGround as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and sa.status in (:statuses)
                        and :excludedItemId is null or sa.id <> :excludedItemId
                        order by sa.orderingId asc
                    """
    )
    Page<BalancingGroupCoordinatorGround> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select max(ra.orderingId) from BalancingGroupCoordinatorGround ra
                    """
    )
    Long findLastOrderingId();

    @Query(
            value = """
                    select sa from BalancingGroupCoordinatorGround as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and ((sa.status in (:statuses))
                        and (:excludedItemId is null or sa.id <> :excludedItemId)
                        or (sa.id in (:includedItemIds)))
                        order by case when sa.id in (:includedItemIds) then 1 else 2 end,
                        sa.defaultSelection desc, sa.orderingId asc
                    """, countQuery = """
            select count(1) from BalancingGroupCoordinatorGround as sa
                where (:prompt is null or (
                    lower(sa.name) like :prompt
                ))
                and ((sa.status in (:statuses))
                and (:excludedItemId is null or sa.id <> :excludedItemId)
                or (sa.id in (:includedItemIds)))
                """
    )
    Page<BalancingGroupCoordinatorGround> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    Optional<BalancingGroupCoordinatorGround> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            """
                    select count (1) from BalancingGroupCoordinatorGround bg
                        where bg.id = :id 
                        and exists( (select 1 from ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator cd
                        join ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult res on res.changeWithdrawalOfCbgId=cd.id
                           where res.balancingGroupCoordinatorGroundId = :id 
                           and cast(cd.status as string) = 'ACTIVE')
                        )"""
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query("""
        select b from BalancingGroupCoordinatorGround b
        where b.id in :ids
""")
    List<BalancingGroupCoordinatorGround> findByIdsIn(List<Long> ids);
}
