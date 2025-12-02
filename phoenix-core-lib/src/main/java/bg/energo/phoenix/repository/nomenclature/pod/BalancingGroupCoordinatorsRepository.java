package bg.energo.phoenix.repository.nomenclature.pod;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.pod.BalancingGroupCoordinators;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BalancingGroupCoordinatorsRepository extends JpaRepository<BalancingGroupCoordinators, Long> {
    @Query("""
        select b
        from BalancingGroupCoordinators b
        where b.id = :id
        and b.status in :statuses
    """)
    Optional<BalancingGroupCoordinators> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            "select b from BalancingGroupCoordinators as b" +
                    " where (:prompt is null or (" +
                    " lower(b.name) like :prompt" +
                    "))" +
                    " and (b.status in (:statuses))" +
                    " and (:excludedItemId is null or b.id <> :excludedItemId) " +
                    " order by b.defaultSelection desc, b.orderingId asc"
    )
    Page<BalancingGroupCoordinators> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "b.id," +
                    " CONCAT(b.name, ' - ', b.fullName), " +
                    " b.orderingId," +
                    " b.defaultSelection," +
                    " b.status" +
                    ") " +
                    "from BalancingGroupCoordinators as b" +
                    " where (:prompt is null or (" +
                    " lower(b.name) like :prompt or " +
                    " lower(b.fullName) like :prompt" +
                    "))" +
                    " and (b.status in (:statuses))" +
                    " order by b.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<BalancingGroupCoordinators> findByDefaultSelectionTrue();

    @Query("select max(b.orderingId) from BalancingGroupCoordinators b")
    Long findLastOrderingId();

    @Query(
            "select b from BalancingGroupCoordinators as b" +
                    " where b.id <> :currentId " +
                    " and (b.orderingId >= :start and b.orderingId <= :end) "
    )
    List<BalancingGroupCoordinators> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select b from BalancingGroupCoordinators as b" +
                    " where b.orderingId is not null" +
                    " order by b.name"
    )
    List<BalancingGroupCoordinators> orderByName();


    @Query(
            """
            select count(b.id) from BalancingGroupCoordinators b
                where lower(b.name) = lower(:name)
                and b.status in (:statuses)
            """
    )
    Long countBankByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id,List<NomenclatureItemStatus> statuses);

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(g.id, g.name)
        from BalancingGroupCoordinators g
        where g.name = :name
        and g.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus(@Param("name")String name, @Param("status") NomenclatureItemStatus status);

    @Query(value = """
            select count(1) from  BalancingGroupCoordinators sa
            where sa.id = :id
            and
            ( exists
            ( select 1 from PointOfDelivery p
              join PointOfDeliveryDetails pd on pd.podId = p.id
                and pd.balancingGroupCoordinatorId = sa.id
              where
                p.status = 'ACTIVE')
            )""")
    Long getActiveConnectionsCount(@Param("id") Long id);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            b.id,
                            b.name
                        )
                        from BalancingGroupCoordinators b
                        where b.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
