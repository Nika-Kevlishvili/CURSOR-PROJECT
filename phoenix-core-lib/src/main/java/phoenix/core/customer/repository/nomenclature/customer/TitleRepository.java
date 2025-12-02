package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.customer.Title;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface TitleRepository extends JpaRepository<Title, Long> {
    @Query(
            "select t from Title as t" +
                    " where (:prompt is null or lower(t.name) like lower(concat('%',:prompt,'%')))" +
                    " and (t.status in (:statuses))" +
                    " and (:excludedItemId is null or t.id <> :excludedItemId) " +
                    " order by t.defaultSelection desc, t.orderingId asc"
    )
    Page<Title> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                    "t.id, " +
                    "t.name, " +
                    "t.orderingId, " +
                    "t.defaultSelection, " +
                    "t.status" +
                    ") " +
                    "from Title as t" +
                    " where (:prompt is null or lower(t.name) like lower(concat('%',:prompt,'%')))" +
                    " and (t.status in (:statuses))" +
                    " order by t.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Title> findByDefaultSelectionTrue();

    @Query("select max(t.orderingId) from Title t")
    Long findLastOrderingId();

    @Query(
            "select t from Title as t" +
                    " where t.id <> :currentId " +
                    " and (t.orderingId >= :start and t.orderingId <= :end) "
    )
    List<Title> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select t from Title as t" +
                    " where t.orderingId is not null" +
                    " order by t.name"
    )
    List<Title> orderByName();

    @Query(
            "select t from Title as t" +
                    " where t.id = :id" +
                    " and t.status in :statuses"
    )
    Optional<Title> findByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatus(Long id, NomenclatureItemStatus status);

    @Query(
            """
            select count(1) from Title t
                where t.id = :id
                and (exists (select 1 from
                    Manager cm,
                    CustomerDetails cd,
                    Customer c
                        where cm.title.id = :id
                        and cm.customerDetailId = cd.id
                        and c.id = cd.customerId
                        and cm.status = 'ACTIVE' and c.status = 'ACTIVE')
                or exists (select 1 from
                    CustomerCommContactPerson cccp,
                    CustomerCommunications cc,
                    CustomerDetails cd,
                    Customer c
                        where cccp.titleId = :id
                        and cc.id = cccp.customerCommunicationsId
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE')
                    )
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
