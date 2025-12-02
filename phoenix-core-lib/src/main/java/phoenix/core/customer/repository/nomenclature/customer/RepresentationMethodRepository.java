package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.customer.RepresentationMethod;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface RepresentationMethodRepository extends JpaRepository<RepresentationMethod, Long> {

    Optional<RepresentationMethod> findByDefaultSelectionTrue();

    @Query(value = "select max(rm.orderingId) from RepresentationMethod rm")
    Long findLastOrderingId();

    @Query(
            "select c from RepresentationMethod as c" +
                    " where (:prompt is null or lower(c.name) like lower(concat('%',:prompt,'%')))" +
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
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(rm.id, rm.name, rm.orderingId, rm.defaultSelection, rm.status) " +
                    "from RepresentationMethod as rm" +
                    " where (:prompt is null or lower(rm.name) like lower(concat('%',:prompt,'%')))" +
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

    @Query(
            "select count(1) from RepresentationMethod rm " +
                    " where rm.id = :id " +
                    " and exists (select 1 from " +
                        "Manager cm, " +
                        "CustomerDetails cd, " +
                        "Customer c" +
                            " where cm.representationMethod.id = :id " +
                            " and cm.customerDetailId = cd.id " +
                            " and cd.customerId = c.id" +
                            " and cm.status = 'ACTIVE' and c.status = 'ACTIVE')"
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
