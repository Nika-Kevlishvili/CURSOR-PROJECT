package bg.energo.phoenix.repository.nomenclature.contract;

import bg.energo.phoenix.model.entity.nomenclature.contract.subActivity.SubActivity;
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
public interface SubActivityRepository extends JpaRepository<SubActivity, Long> {
    @Query(value = "select max(r.orderingId) from SubActivity r")
    Long findLastOrderingId();

    @Query(
            """
                    select count(c.id) from SubActivity c
                        where lower(c.name) = lower(:name)
                        and c.status in (:statuses)
                    """
    )
    Long countSubActivityByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<SubActivity> findByDefaultSelectionTrue();

    @Query("select r from SubActivity as r " +
           " left join r.activity as rc " +
           " where (r.status in (:statuses))" +
           " and (:prompt is null or (" +
           " lower(r.name) like :prompt)) " +
           " and (:activityId is null or rc.id = :activityId) " +
           " and (:excludedItemId is null or r.id <> :excludedItemId) " +
           " order by r.defaultSelection desc, r.orderingId asc "
    )
    Page<SubActivity> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("activityId") Long activityId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select r from SubActivity as r" +
            " where r.orderingId is not null" +
            " order by r.name"
    )
    List<SubActivity> orderByName();

    @Query(
            "select r from SubActivity as r" +
            " where r.id <> :currentId " +
            " and (r.orderingId >= :start and r.orderingId <= :end) "
    )
    List<SubActivity> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
            "r.id, " +
            "CONCAT(r.name, ' - ', rc.name), " +
            "r.orderingId, " +
            "r.defaultSelection, " +
            "r.status" +
            ") " +
            "from SubActivity as r" +
            " left join r.activity as rc " +
            " where (r.status in (:statuses))" +
            " and (:prompt is null or (" +
            "lower(r.name) like :prompt)) " +
            " and (:excludedItemId is null or r.id <> :excludedItemId) " +
            " order by r.defaultSelection desc,r.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    Optional<SubActivity> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from SubActivity s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
            select count(pca.id) > 0
            from ProductContractActivity pca
            join SystemActivity sac on pca.systemActivityId = sac.id
            join SubActivity sa on sac.subActivityId = sa.id
            where pca.status = 'ACTIVE'
            and sac.status = 'ACTIVE'
            and sa.id = :id
            """)
    boolean hasActiveConnectionsWithProductContract(Long id);

    @Query("""
            select count(sca.id) > 0
            from ServiceContractActivity sca
            join SystemActivity sac on sca.systemActivityId = sac.id
            join SubActivity sa on sac.subActivityId = sa.id
            where sca.status = 'ACTIVE'
            and sac.status = 'ACTIVE'
            and sa.id = :id
            """)
    boolean hasActiveConnectionsWithServiceContracts(Long id);

    @Query("""
            select count(soa.id) > 0
            from ServiceOrderActivity soa
            join SystemActivity sac on soa.systemActivityId = sac.id
            join SubActivity sa on sac.subActivityId = sa.id
            where soa.status = 'ACTIVE'
            and sac.status = 'ACTIVE'
            and sa.id = :id
            """)
    boolean hasActiveConnectionsWithServiceOrder(Long id);

    @Query("""
            select count(goa.id) > 0
            from GoodsOrderActivity goa
            join SystemActivity sac on goa.systemActivityId = sac.id
            join SubActivity sa on sac.subActivityId = sa.id
            where goa.status = 'ACTIVE'
            and sac.status = 'ACTIVE'
            and sa.id = :id
            """)
    boolean hasActiveConnectionsWithGoodsOrder(Long id);

}
