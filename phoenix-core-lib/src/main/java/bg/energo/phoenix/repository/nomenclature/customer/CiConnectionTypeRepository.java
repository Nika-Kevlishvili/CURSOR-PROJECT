package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.CiConnectionType;
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

public interface CiConnectionTypeRepository extends JpaRepository<CiConnectionType, Long> {
    @Query(
            "select c from CiConnectionType as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<CiConnectionType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(c.id, c.name, c.orderingId, c.defaultSelection, c.status) " +
                    "from CiConnectionType as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " order by c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<CiConnectionType> findByDefaultSelectionTrue();

    @Query("select max(c.orderingId) from CiConnectionType c")
    Long findLastOrderingId();

    @Query(
            "select c from CiConnectionType as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<CiConnectionType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from CiConnectionType as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<CiConnectionType> orderByName();

    @Query(
            "select count(1) from CiConnectionType cct " +
                    " where cct.id = :id " +
                    " and exists (select 1 from RelatedCustomer rc, Customer c " +
                        " where rc.ciConnectionType.id = :id " +
                        " and rc.customerId = c.id " +
                        " and rc.status = 'ACTIVE' and c.status = 'ACTIVE')"
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query("""
        select new bg.energo.phoenix.model.CacheObject(c.id, c.name)
        from CiConnectionType c
        where c.name = :name
        and c.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getByNameAndStatus(@Param("name") String name,
                                             @Param("status") NomenclatureItemStatus status);

    Optional<CiConnectionType> findByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            """
            select count(c.id) from CiConnectionType c
                where lower(c.name) = lower(:name)
                and c.status in (:statuses)
            """
    )
    Long countCiConnectionTypeByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from CiConnectionType c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
