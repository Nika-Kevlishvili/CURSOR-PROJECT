package bg.energo.phoenix.repository.nomenclature.product;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    boolean existsByIdAndStatus(Long id, NomenclatureItemStatus status);

    @Query("select c from Currency as c " +
            "left join c.altCurrency as ac " +
            "where (c.status='ACTIVE' or c.status = 'INACTIVE') " +
            " and ac.id = :altCurrencyId ")
    List<Currency> getAllByAltCurrencyId(@Param("altCurrencyId") Long altCurrencyId);

    @Query("select c from Currency as c" +
            " left join c.altCurrency as ac " +
            " where (c.status in (:statuses))" +
            " and (:prompt is null or (" +
            " lower(c.name) like :prompt or " +
            " lower(c.fullName) like :prompt or " +
            " lower(c.printName) like :prompt or " +
            " lower(c.abbreviation) like :prompt)) " +
            " and (:altCurrencyId is null or ac.id = :altCurrencyId)" +
            " and (:excludedItemId is null or c.id <> :excludedItemId) " +
            " order by c.defaultSelection desc, c.orderingId asc "
    )
    Page<Currency> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("altCurrencyId") Long altCurrencyId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query("select max(c.orderingId) from Currency c")
    Long findLastOrderingId();

    @Query(
            "select count (c.id) from Currency c" +
                    " where c.status = 'ACTIVE' or c.status = 'INACTIVE'"
    )
    Long countByActiveStatus();

    Optional<Currency> findByDefaultSelectionTrue();

    @Query("""
                    select new bg.energo.phoenix.model.CacheObject(c.id, c.name) from Currency c
                    where c.defaultSelection = true
            """)
    Optional<CacheObject> findByDefaultSelectionTrueCache();

//    Optional<Currency> findCurrencyByNameAndStatus(String name, NomenclatureItemStatus status);

    @Query(
            """
                    select count(c.id) from Currency c
                        where lower(c.name) = lower(:name)
                        and c.status in (:statuses)
                    """
    )
    Long countCurrencyByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

//    Optional<Currency> findCurrencyByMainCurrencyStartDateAndMainCurrencyAndStatus(LocalDate mainCurrencyStartDate, boolean mainCurrency, NomenclatureItemStatus status);

    @Query("select c from Currency as c" +
            " where c.mainCurrencyStartDate = :mainCurrencyStartDate" +
            " and c.mainCurrency = :mainCurrency" +
            " and c.status in (:statuses)"
    )
    Optional<Currency> findCurrencyByMainCurrencyStartDateAndMainCurrencyAndStatuses
            (@Param("mainCurrencyStartDate") LocalDate mainCurrencyStartDate,
             @Param("mainCurrency") boolean mainCurrency,
             @Param("statuses") List<NomenclatureItemStatus> status);

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "c.id, " +
                    "c.name, " +
                    "c.orderingId, " +
                    "c.defaultSelection, " +
                    "c.status" +
                    ") " +
                    "from Currency as c" +
                    " left join c.altCurrency as ac " +
                    " where (c.status in (:statuses))" +
                    " and (:prompt is null or (" +
                    " lower(c.name) like :prompt or " +
                    " lower(c.fullName) like :prompt or " +
                    " lower(c.printName) like :prompt or " +
                    " lower(c.abbreviation) like :prompt)) " +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select c from Currency as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<Currency> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from Currency as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<Currency> orderByName();

    @Query(
            "select count (c.id) from Currency c" +
                    " where c.status = 'ACTIVE' and c.mainCurrency = true"
    )
    Long countByMainCurrencyAndStatus();

    @Query("""
             select c from Currency c
             where c.id = :id and c.status in(:statuses)
            """
    )
    Optional<Currency> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
             select c from Currency c
             where c.id in(:ids) and c.status in(:statuses)
            """
    )
    List<Currency> findByIdsAndStatus(
            @Param("ids") Set<Long> ids,
            @Param("statuses") List<NomenclatureItemStatus> statuses);


    @Query(value = """
            select count(sa.id) > 0 as is_used
            from nomenclature.currencies sa
            where sa.id = :id
              and (exists
                       (select 1
                        from product.products p
                                 join product.product_details pd on pd.product_id = p.id
                            and pd.currency_id = sa.id
                        where pd.status in ('ACTIVE', 'INACTIVE')
                          and p.status = 'ACTIVE')
                or
                   exists
                       (select 1
                        from service.services s
                                 join service.service_details sd on sd.service_id = s.id
                            and sd.currency_id = sa.id
                        where sd.status in ('ACTIVE', 'INACTIVE')
                          and s.status = 'ACTIVE')
                or
                   exists
                       (select 1
                        from goods.goods g
                                 join goods.goods_details gd on gd.goods_id = g.id
                            and gd.currency_id = sa.id
                        where gd.status in ('ACTIVE', 'INACTIVE')
                          and g.status = 'ACTIVE')
                or
                   exists
                       (select 1
                        from price_component.price_components pc
                        where pc.currency_id = sa.id
                          and pc.status in ('ACTIVE'))
                or
                   exists
                       (select 1
                        from interim_advance_payment.interim_advance_payments ic
                        where ic.currency_id = sa.id
                          and ic.status in ('ACTIVE'))
                or
                   exists
                       (select 1
                        from terms.penalties pec
                        where pec.currency_id = sa.id
                          and pec.status in ('ACTIVE'))
                or
                   exists
                       (select 1
                        from pod.discounts d
                        where d.currency_id = sa.id
                          and d.status in ('ACTIVE'))
                or
                   exists
                       (select 1
                        from billing.billings run
                        left join billing.billing_detailed_data bdd on bdd.billing_id = run.id
                        left join billing.billing_summary_data bsd on bsd.billing_id = run.id
                        where (
                            run.amount_excluding_vat_currency_id = sa.id
                            or bdd.value_currency_id = sa.id
                            or bsd.value_currency_id = sa.id
                        )
                        and run.status <> 'DELETED')
                or
                   exists
                       (select 1
                        from receivable.customer_liabilities cl
                        where cl.currency_id = sa.id
                          and cl.status = 'ACTIVE')
                or
                   exists
                       (select 1
                        from receivable.customer_payments cp
                        where cp.currency_id = sa.id
                          and cp.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.customer_receivables cr
                          where cr.customer_id = sa.id
                            and cr.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.customer_deposits cd
                          where cd.currency_id = sa.id
                            and cd.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.collection_channels cc
                          where cc.currency_id = sa.id
                            and cc.status = 'ACTIVE')
                or
                   exists(select 1
                          from interest_rate.interest_rates ir
                          where ir.currency_id = sa.id
                            and ir.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.reminders r
                          where r.currency_id = sa.id
                            and r.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.power_supply_disconnection_reminders r
                          where r.currency_id = sa.id
                            and r.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.power_supply_disconnection_requests r
                          where r.currency_id = sa.id
                            and r.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.reschedulings r
                          where r.currency_id = sa.id
                            and r.status = 'ACTIVE')
                or
                   exists(select 1
                          from receivable.late_payment_fines f
                          where f.currency_id = sa.id
                            and f.currency_id = sa.id)
                or
                   exists(select 1
                          from receivable.mass_operation_for_blocking b
                          where b.currency_id = sa.id
                            and b.status = 'ACTIVE')
                or
                   exists(select 1
                          from nomenclature.grid_operator_taxes t
                          where t.currency_id = sa.id
                            and t.status in ('ACTIVE', 'INACTIVE'))
                )
            """, nativeQuery = true)
    boolean hasActiveConnections(@Param("id") Long id);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from Currency c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(c.id, c.name)
                from Currency c
                where c.name = :name
                and c.status =:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatus(@Param("name") String name, @Param("status") NomenclatureItemStatus status);

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(c.id, c.name)
                from Currency c
                where c.name = :name
                and c.status in :status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByNameAndStatusIn(@Param("name") String name, @Param("status") List<NomenclatureItemStatus> status);

    @Query("""
            select c
            from Currency c
            where c.id = :id
            and c.status in :statuses
            """)
    Optional<Currency> findCurrencyByIdAndStatuses(@Param("id") Long id,
                                                   @Param("statuses") List<NomenclatureItemStatus> statuses);

    Optional<Currency> findByNameAndStatusIn(String name, List<NomenclatureItemStatus> status);

    @Query(value = """
               select * from nomenclature.currencies
               where main_ccy_start_date <= current_date
                   and main_ccy = true
                   and status ='ACTIVE'
            order by main_ccy_start_date desc
               limit 1""",
            nativeQuery = true)
    Optional<Currency> findMainCurrencyNowAndActive();

    Optional<Currency> findByDefaultSelectionIsTrue();
}
