package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.customer.Segment;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Long> {

    @Query("""
        select s
        from Segment s
        where s.id = :id
        and s.status in :statuses
    """)
    Optional<Segment> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query(
            "select s from Segment as s" +
                    " where (:prompt is null or lower(s.name) like lower(concat('%',:prompt,'%')))" +
                    " and (s.status in (:statuses))" +
                    " and (:excludedItemId is null or s.id <> :excludedItemId) " +
                    " order by s.defaultSelection desc, s.orderingId asc"
    )
    Page<Segment> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                    "s.id," +
                    "s.name, " +
                    "s.orderingId, " +
                    "s.defaultSelection, " +
                    "s.status" +
                    ") " +
                    "from Segment as s" +
                    " where (:prompt is null or lower(s.name) like lower(concat('%',:prompt,'%')))" +
                    " and (s.status in (:statuses))" +
                    " order by s.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<Segment> findByDefaultSelectionTrue();

    @Query("select max(s.orderingId) from Segment s")
    Long findLastOrderingId();

    @Query(
            "select s from Segment as s" +
                    " where s.id <> :currentId " +
                    " and (s.orderingId >= :start and s.orderingId <= :end) "
    )
    List<Segment> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select s from Segment as s" +
                    " where s.orderingId is not null" +
                    " order by s.name"
    )
    List<Segment> orderByName();

    @Query(
            """
            select count(1) from Segment s
                where s.id = :id
                and exists (select 1 from CustomerSegment cs, CustomerDetails cd, Customer c
                    where cs.segment.id = :id
                    and cs.customerDetail.id = cd.id
                    and cd.customerId = c.id
                    and cs.status = 'ACTIVE' and c.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
