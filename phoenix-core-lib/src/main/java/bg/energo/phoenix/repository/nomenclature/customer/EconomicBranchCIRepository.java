package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.EconomicBranchCI;
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
public interface EconomicBranchCIRepository extends JpaRepository<EconomicBranchCI, Long> {

    @Query("""
        select e
        from EconomicBranchCI e
        where e.id = :id
        and e.status in :statuses
    """)
    Optional<EconomicBranchCI> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(e.id, e.name)
        from EconomicBranchCI e
        where e.name = :name
        and e.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findByNameAndStatus(
            @Param("name") String name,
            @Param("status") NomenclatureItemStatus status
    );
    @Query(
            "select e from EconomicBranchCI as e" +
                    " where (:prompt is null or lower(e.name) like :prompt)" +
                    " and (e.status in (:statuses))" +
                    " and (:excludedItemId is null or e.id <> :excludedItemId) " +
                    " order by e.defaultSelection desc, e.orderingId asc"
    )
    Page<EconomicBranchCI> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "e.id, " +
                    "e.name, " +
                    "e.orderingId, " +
                    "e.defaultSelection, " +
                    "e.status" +
                    ") " +
                    "from EconomicBranchCI as e" +
                    " where (:prompt is null or lower(e.name) like :prompt)" +
                    " and (e.status in (:statuses))" +
                    " order by e.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<EconomicBranchCI> findByDefaultSelectionTrue();

    @Query("select max(e.orderingId) from EconomicBranchCI e")
    Long findLastOrderingId();

    @Query(
            "select e from EconomicBranchCI as e" +
                    " where e.id <> :currentId " +
                    " and (e.orderingId >= :start and e.orderingId <= :end) "
    )
    List<EconomicBranchCI> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select e from EconomicBranchCI as e" +
                    " where e.orderingId is not null" +
                    " order by e.name"
    )
    List<EconomicBranchCI> orderByName();

    @Query(
            """
            select count(1) from EconomicBranchCI ebci
                where ebci.id = :id
                and exists (select 1 from CustomerDetails cd, Customer c
                    where cd.economicBranchCiId = :id and cd.customerId = c.id
                    and c.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query(
            """
            select count(e.id) from EconomicBranchCI e
                where lower(e.name) = lower(:name)
                and e.status in (:statuses)
            """
    )
    Long countEconomicBranchCIByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            e.id,
                            e.name
                        )
                        from EconomicBranchCI e
                        where e.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
