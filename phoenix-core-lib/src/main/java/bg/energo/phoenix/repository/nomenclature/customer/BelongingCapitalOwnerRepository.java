package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.BelongingCapitalOwner;
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
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BelongingCapitalOwnerRepository extends JpaRepository<BelongingCapitalOwner, Long> {
    @Query(
            "select b from BelongingCapitalOwner as b" +
                    " where (:prompt is null or lower(b.name) like :prompt)" +
                    " and (b.status in (:statuses))" +
                    " and (:excludedItemId is null or b.id <> :excludedItemId) " +
                    " order by b.defaultSelection desc, b.orderingId asc"
    )
    Page<BelongingCapitalOwner> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "b.id, " +
                    "b.name, " +
                    "b.orderingId, " +
                    "b.defaultSelection, " +
                    "b.status" +
                    ") " +
                    "from BelongingCapitalOwner as b" +
                    " where (:prompt is null or lower(b.name) like :prompt)" +
                    " and (b.status in (:statuses))" +
                    " order by b.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<BelongingCapitalOwner> findByDefaultSelectionTrue();

    @Query("select max(b.orderingId) from BelongingCapitalOwner b")
    Long findLastOrderingId();

    @Query(
            "select b from BelongingCapitalOwner as b" +
                    " where b.id <> :currentId " +
                    " and (b.orderingId >= :start and b.orderingId <= :end) "
    )
    List<BelongingCapitalOwner> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select b from BelongingCapitalOwner as b" +
                    " where b.orderingId is not null" +
                    " order by b.name"
    )
    List<BelongingCapitalOwner> orderByName();


    @Query("select b from BelongingCapitalOwner b where b.id = :id and b.status in :statuses")
    Optional<BelongingCapitalOwner> findByIdAndStatuses(@Param("id") Long id,@Param("statuses") List<NomenclatureItemStatus> statuses);


    @Query("""
        select new bg.energo.phoenix.model.CacheObject(b.id, b.name)
        from BelongingCapitalOwner b
        where b.name = :name
        and b.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getByNameAndStatus(@Param("name") String name,
                                             @Param("status") NomenclatureItemStatus status);

    @Query(
            "select count(1) from BelongingCapitalOwner bco " +
                    " where bco.id = :id " +
                    " and exists (select 1 from CustomerOwner co, Customer c " +
                        " where co.belongingCapitalOwner.id = :id " +
                        " and co.customer.id = c.id " +
                        " and co.status = 'ACTIVE'" +
                        " and c.status = 'ACTIVE')"
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query(
            """
            select count(b.id) from BelongingCapitalOwner b
                where lower(b.name) = lower(:name)
                and b.status in (:statuses)
            """
    )
    Long countBelongingCapitalOwnerByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            b.id,
                            b.name
                        )
                        from BelongingCapitalOwner b
                        where b.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
