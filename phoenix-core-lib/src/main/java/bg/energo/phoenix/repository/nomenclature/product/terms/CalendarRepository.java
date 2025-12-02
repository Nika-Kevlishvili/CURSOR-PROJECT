package bg.energo.phoenix.repository.nomenclature.product.terms;

import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    boolean existsByIdAndStatus(Long id, NomenclatureItemStatus status);
    @Query("""
             select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                 c.id,
                 c.name,
                 c.orderingId,
                 c.defaultSelection,
                 c.status
             ) from Calendar c
             where (c.status in (:statuses))
             and (:prompt is null or (lower(c.name)) like :prompt)
             and (:excludedItemId is null or c.id <> :excludedItemId)
             order by c.defaultSelection desc, c.orderingId asc
            """)
    Page<NomenclatureResponse> filterNomenclature(@Param("prompt") String prompt,
                                                  @Param("statuses") List<NomenclatureItemStatus> statuses,
                                                  @Param("excludedItemId") Long excludedItemId,
                                                  PageRequest pageRequest);

    @Query("""
            select c from Calendar as c
            where c.id <> :currentId
            and (c.orderingId >= :start and c.orderingId <= :end)
            order by c.orderingId
            """)
    List<Calendar> findInOrderingIdRange(@Param("start") Long start, @Param("end") Long end, @Param("currentId") Long id, Sort orderingId);

    @Query("""
            select c from Calendar as c
            where c.orderingId is not null
            order by c.name
            """)
    List<Calendar> orderByName();

    @Query("""
            select count(c.id) from Calendar c
            where c.name like (:name)
            and c.status in (:statuses)
            """)
    long countByNameAndStatuses(String name, List<NomenclatureItemStatus> statuses);

    @Query("""
            select max(c.orderingId) from Calendar c
            """)
    Long findLastOrderingId();

    Optional<Calendar> findByDefaultSelectionTrue();

    @Query("""
            select new bg.energo.phoenix.model.response.nomenclature.terms.CalendarResponse(c) from Calendar c
            where (:prompt is null or (lower(c.name) like lower((concat('%', :prompt, '%')))))
            and (c.status in(:statuses))
            and (:excludedItemId is null or c.id <> :excludedItemId)
            order by c.defaultSelection desc, c.orderingId asc
            """)
    Page<CalendarResponse> filter(@Param("prompt") String prompt, @Param("statuses") List<NomenclatureItemStatus> statuses, @Param("excludedItemId") Long excludedItemId, PageRequest pageRequest);

    Optional<Calendar> findByIdAndStatusIsIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query("""
            select c.id from Calendar c
            where c.id in(:ids)
            and c.status in(:statuses)
            """)
    List<Long> findByIdInAndStatusIsIn(List<Long> ids, List<NomenclatureItemStatus> statuses);

    @Query(nativeQuery = true, value = """  
            select distinct tbl.obj

            from (select 'Invoice Payment Terms' as obj
                  from terms.invoice_payment_terms t
                  where t.status = 'ACTIVE'
                    and t.calendar_id = :id

                  union all

                  select 'Process Periodicity' as obj
                  from billing.process_periodicity pp
                  where pp.status = 'ACTIVE'
                    and pp.calendar_id = :id

                  union all

                  select 'Deposit' as obj
                  from receivable.customer_deposits cd
                           join receivable.customer_deposit_payment_ddl_aft_withdrawal pd
                                on cd.id = pd.customer_deposit_id
                  where cd.status = 'ACTIVE'
                    and pd.status = 'ACTIVE'
                    and pd.calendar_id = :id

                  union all

                  select 'Collection Channel' as obj
                  from receivable.collection_channels cc
                  where cc.status = 'ACTIVE'
                    and cc.calendar_id = :id

                  union all

                  select 'Interest Rate' as obj
                  from interest_rate.interest_rates ir
                           join interest_rate.interest_rate_payment_terms irpt
                                on ir.id = irpt.interest_rate_id
                  where ir.status = 'ACTIVE'
                    and irpt.status = 'ACTIVE'
                    and irpt.calendar_id = :id

                  union all

                  select 'Task Type' as obj
                  from nomenclature.task_types tt
                  where tt.status = 'ACTIVE'
                    and tt.calendar_id = :id) as tbl
            limit 1
           """)
    Optional<String> activeConnection(@Param("id") Long id);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from Calendar c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
