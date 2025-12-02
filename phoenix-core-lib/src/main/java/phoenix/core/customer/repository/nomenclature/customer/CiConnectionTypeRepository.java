package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.customer.CiConnectionType;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface CiConnectionTypeRepository extends JpaRepository<CiConnectionType, Long> {
    @Query(
            "select c from CiConnectionType as c" +
                    " where (:prompt is null or lower(c.name) like lower(concat('%',:prompt,'%')))" +
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
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(c.id, c.name, c.orderingId, c.defaultSelection, c.status) " +
                    "from CiConnectionType as c" +
                    " where (:prompt is null or lower(c.name) like lower(concat('%',:prompt,'%')))" +
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
}
