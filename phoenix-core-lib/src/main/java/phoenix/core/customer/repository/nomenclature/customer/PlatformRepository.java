package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.customer.Platform;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface PlatformRepository extends JpaRepository<Platform, Long> {
    @Query(
            "select p from Platform  as p" +
                    " where (:prompt is null or lower(p.name) like lower(concat('%',:prompt,'%')))" +
                    " and (p.status in (:statuses))" +
                    " and (:excludedItemId is null or p.id <> :excludedItemId) " +
                    " order by p.defaultSelection desc, p.orderingId asc"
    )
    Page<Platform> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                        "p.id, " +
                        "p.name, " +
                        "p.orderingId, " +
                        "p.defaultSelection, " +
                        "p.status" +
                    ") " +
                    "from Platform as p" +
                    " where (:prompt is null or lower(p.name) like lower(concat('%',:prompt,'%')))" +
                    " and (p.status in (:statuses))" +
                    " order by p.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Platform> findByDefaultSelectionTrue();

    @Query("select max(p.orderingId) from Platform  p")
    Long findLastOrderingId();

    @Query(
            "select p from Platform as p" +
                    " where p.id <> :currentId " +
                    " and (p.orderingId >= :start and p.orderingId <= :end) "
    )
    List<Platform> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select p from Platform as p" +
                    " where p.orderingId is not null" +
                    " order by p.name"
    )
    List<Platform> orderByName();

    boolean existsByIdAndStatus(Long id, NomenclatureItemStatus status);

    @Query(
            """
            select count(1) from Platform p
                where p.id = :id
                and exists (select 1 from 
                    CustomerCommunicationContacts ccc, 
                    CustomerCommunications cc, 
                    CustomerDetails cd, 
                    Customer c
                        where ccc.platformId = :id
                        and cc.id = ccc.customerCommunicationsId
                        and cd.id = cc.customerDetailsId
                        and cd.customerId = c.id
                        and ccc.status = 'ACTIVE' and cc.status = 'ACTIVE' and c.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
