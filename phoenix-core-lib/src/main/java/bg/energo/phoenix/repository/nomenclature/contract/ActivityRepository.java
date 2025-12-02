package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.Activity;
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
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    @Query(
            "select c from Activity as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<Activity> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "c.id, " +
                    "c.name, " +
                    "c.orderingId, " +
                    "c.defaultSelection, " +
                    "c.status" +
                    ") " +
                    "from Activity as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Activity> findByDefaultSelectionTrue();

    @Query(
            "select c from Activity as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<Activity> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from Activity as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<Activity> orderByName();

    @Query(
            """
            select count(c.id) from Activity c
                where lower(c.name) = lower(:name)
                and c.status in (:statuses)
            """
    )
    Long countActivityByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query("select max(c.orderingId) from Activity c")
    Long findLastOrderingId();


    Optional<Activity> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            a.id,
                            a.name
                        )
                        from Activity a
                        where a.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
            select count(pca.id) > 0
                from ProductContractActivity pca
                join SystemActivity sa on sa.id = pca.systemActivityId
                join Activity ac on ac.id = sa.activityId
                    where ac.id = :id
                    and pca.status = 'ACTIVE'
            """
    )
    boolean hasActiveConnectionsToProductContract(
            @Param("id") Long id
    );

    @Query(
            """
            select count(sca.id) > 0
                from ServiceContractActivity sca
                join SystemActivity  sa on sa.id = sca.systemActivityId
                join Activity ac on ac.id = sa.activityId
                    where ac.id = :id
                    and sca.status = 'ACTIVE'
                """
    )
    boolean hasActiveConnectionsToServiceContract(
            @Param("id") Long id
    );


    @Query(
            value = """
                    select count(soa.id) > 0
                        from ServiceOrderActivity soa
                        join SystemActivity sa on sa.id = soa.systemActivityId
                        join Activity ac on ac.id = sa.activityId
                            where ac.id = :id
                            and soa.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionsToServiceOrder(
            @Param("id") Long id
    );


    @Query(
            value = """
                    select count(goa.id) > 0
                        from GoodsOrderActivity goa
                        join SystemActivity sa on sa.id = goa.systemActivityId
                        join Activity ac on ac.id = sa.activityId
                            where ac.id = :id
                            and goa.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionsToGoodsOrder(
            @Param("id") Long id
    );

    @Query("""
            select count(sa.id) > 0
            from SubActivity sa
            join Activity a on sa.activity.id = a.id
            where sa.status = 'ACTIVE'
            and a.id = :id
            """)
    boolean hasActiveConnectionsToSubActivity(@Param("id") Long id);
}
