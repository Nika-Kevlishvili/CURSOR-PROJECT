package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.RepresentationMethod;
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

public interface RepresentationMethodRepository extends JpaRepository<RepresentationMethod, Long> {

    Optional<RepresentationMethod> findByDefaultSelectionTrue();

    @Query(value = "select max(rm.orderingId) from RepresentationMethod rm")
    Long findLastOrderingId();

    @Query(
            "select c from RepresentationMethod as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<RepresentationMethod> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(rm.id, rm.name, rm.orderingId, rm.defaultSelection, rm.status) " +
                    "from RepresentationMethod as rm" +
                    " where (:prompt is null or lower(rm.name) like :prompt)" +
                    " and (rm.status in (:statuses))" +
                    " order by rm.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );
    @Query(
            "select rm from RepresentationMethod as rm " +
             " where rm.id <> :currentId " +
             " and (rm.orderingId >= :start and rm.orderingId <= :end)"
    )
    List<RepresentationMethod> findInOrderingIdRange(@Param("start") Long start,
                                                     @Param("end")Long end,
                                                     @Param("currentId") Long currentId,
                                                     Sort sort);

    @Query(
            "select rm from RepresentationMethod as rm "+
                    " where rm.orderingId is not null "+
                    " order by rm.name"
    )
    List<RepresentationMethod> orderByName();

    @Query(
            "select rm from RepresentationMethod as rm" +
                    " where rm.id = :id" +
                    " and rm.status in :statuses"
    )
    Optional<RepresentationMethod> findByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );


    @Query("""
        select new bg.energo.phoenix.model.CacheObject(r.id, r.name)
        from RepresentationMethod r
        where r.name = :name
        and r.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getByNameAndStatus(@Param("name") String name,
                                             @Param("status") NomenclatureItemStatus status);

    @Query(
            "select count(1) from RepresentationMethod rm " +
                    " where rm.id = :id " +
                    " and exists (select 1 from " +
                        "Manager m, " +
                        "CustomerDetails cd, " +
                        "Customer c" +
                            " where m.representationMethod.id = :id " +
                            " and m.customerDetailId = cd.id " +
                            " and cd.customerId = c.id" +
                            " and m.status = 'ACTIVE' and c.status = 'ACTIVE')"
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query(
            """
            select count(r.id) from RepresentationMethod r
                where lower(r.name) = lower(:name)
                and r.status in (:statuses)
            """
    )
    Long countRepresentationMethodByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            r.id,
                            r.name
                        )
                        from RepresentationMethod r
                        where r.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
