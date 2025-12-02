package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.customer.ContactPurpose;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface ContactPurposeRepository extends JpaRepository<ContactPurpose, Long> {

    @Query(
            "select c from ContactPurpose as c" +
                    " where (:prompt is null or lower(c.name) like lower(concat('%',:prompt,'%')))" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<ContactPurpose> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );
    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                    "c.id, " +
                    "c.name, " +
                    "c.orderingId, " +
                    "c.defaultSelection, " +
                    "c.status" +
                    ") " +
                    "from ContactPurpose as c" +
                    " where (:prompt is null or lower(c.name) like lower(concat('%',:prompt,'%')))" +
                    " and (c.status in (:statuses))" +
                    " order by c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<ContactPurpose> findByDefaultSelectionTrue();

    @Query("select max(c.orderingId) from ContactPurpose c")
    Long findLastOrderingId();

    @Query(
            "select c from ContactPurpose as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<ContactPurpose> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from ContactPurpose as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<ContactPurpose> orderByName();

    boolean existsByIdAndStatus(Long id, NomenclatureItemStatus status);

    @Query(
            "select cp from ContactPurpose as cp" +
                    " where cp.id = :id" +
                    " and cp.status in :statuses"
    )
    Optional<ContactPurpose> findByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            "select count(1) from ContactPurpose cp " +
                    " where cp.id = :id " +
                    " and exists (select 1 from " +
                        " CustomerCommContactPurposes cccp," +
                        " CustomerCommunications cc," +
                        " CustomerDetails cd," +
                        " Customer c " +
                            " where cccp.contactPurposeId = :id " +
                            " and cc.id = cccp.customerCommunicationsId " +
                            " and cd.id = cc.customerDetailsId " +
                            " and cccp.status = 'ACTIVE' and cc.status = 'ACTIVE' and c.status = 'ACTIVE' )"
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
