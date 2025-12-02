package bg.energo.phoenix.repository.nomenclature.receivable;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BlockingReason;
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
public interface BlockingReasonRepository extends JpaRepository<BlockingReason, Long> {

    @Query("""
             select c from BlockingReason c
             where c.id = :id and c.status in(:statuses)
            """
    )
    Optional<BlockingReason> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query("""
             select c.id from BlockingReason c
             where c.name = :name and c.status = 'ACTIVE' and c.isHardCoded
            """
    )
    Long findByNameAndHardCodedTrue(String name);

    @Query("""
             select c from BlockingReason c
             where c.id = :id
             and c.status in(:statuses)
             and lower((cast(c.reasonTypes as string))) like lower(text(:reasonType)) 
            """
    )
    Optional<BlockingReason> findByIdAndStatusAndReasonType(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> status,
            @Param("reasonType") String reasonType
    );

    @Query("""
            select br from BlockingReason br
            where br.name like(:name)
            and br.status in(:statuses)
            """)
    List<BlockingReason> findByNameAndStatuses(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("select max(br.orderingId) from BlockingReason br")
    Long findLastOrderingId();

    Optional<BlockingReason> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                           br.id,
                            br.name
                        )
                        from BlockingReason br
                        where br.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            "select br from BlockingReason as br" +
            " where br.id <> :currentId " +
            " and (br.orderingId >= :start and br.orderingId <= :end) "
    )
    List<BlockingReason> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select br from BlockingReason as br
                where br.orderingId is not null
                order by br.name
            """
    )
    List<BlockingReason> orderByName();

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(br.id, br.name, br.orderingId, br.defaultSelection, br.status) " +
                    "from BlockingReason as br" +
                    " where (:prompt is null or lower(br.name) like :prompt)" +
                    " and (:excludedItemId is null or br.id <> :excludedItemId) " +
                    " and (br.status in (:statuses))" +
                    " order by br.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select br from BlockingReason as br" +
            " where (:prompt is null or lower(br.name) like :prompt) " +
            " and (br.status in (:statuses))" +
            " and (:type is null or :type = 'ALL' or text(br.reasonTypes) like concat('%', :type, '%'))" +
            " and (:excludedItemId is null or br.id <> :excludedItemId) " +
            " order by " +
            " case when br.isHardCoded = true then 0 " +
            "      when br.defaultSelection = true then 1 " +
            "      else 2 " +
            " end, " +
            " br.orderingId asc"
    )
    Page<BlockingReason> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("type") String type,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(br.id,br.name)
            from BlockingReason br
            where br.name=:name
            and br.status=:status
             """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(String name, NomenclatureItemStatus status);

    @Query(nativeQuery = true, value = """
            select distinct tbl.obj

            from (select 'Liability' as obj
                  from receivable.customer_liabilities l
                  where l.status = 'ACTIVE'
                    and (l.blocked_for_calculation_of_late_payment_blocking_reason_id = :id
                      or l.blocked_for_liabilities_offsetting_blocking_reason_id = :id
                      or l.blocked_for_payment_blocking_reason_id = :id
                      or l.blocked_for_reminder_letters_blocking_reason_id = :id
                      or l.blocked_for_supply_termination_blocking_reason_id = :id)

                  union all

                  select 'Payment' as obj
                  from receivable.customer_payments cp
                  where cp.status = 'ACTIVE'
                    and cp.blocked_for_offsetting_blocking_reason_id = :id

                  union all

                  select 'Receivable' as obj
                  from receivable.customer_receivables cr
                  where cr.status = 'ACTIVE'
                    and cr.blocked_for_payment_blocking_reason_id = :id

                  union all

                  select 'Mass Operation for Blocking' as obj
                  from receivable.mass_operation_for_blocking b
                  where b.status = 'ACTIVE'
                    and (b.blocked_for_payment_blocking_reason_id = :id
                      or b.blocked_for_supply_termination_blocking_reason_id = :id
                      or b.blocked_for_reminder_letters_blocking_reason_id = :id
                      or b.blocked_for_liabilities_offsetting_blocking_reason_id = :id
                      or b.blocked_for_calculation_of_late_payment_blocking_reason_id = :id)) as tbl
            limit 1
            """)
    Optional<String> activeConnections(@Param("id") Long id);
}
