package bg.energo.phoenix.repository.billing.accountingPeriods;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountPeriodSapResponse;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountPeriodVatResponse;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsListingMiddleResponse;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingPeriodsRepository extends JpaRepository<AccountingPeriods, Long> {
    @Query(nativeQuery = true,
            value = """
                       select
                             ap.name as name,
                             ap.start_date as startDate,
                             ap.end_date as endDate,
                             ap.status as status,
                             ap.closed_on_date as closedOnDate,
                             ap.account_period_id as accountPeriodId
                         from(
                                 select
                                     ap.name,
                                     ap.start_date,
                                     ap.end_date,
                                     ap.status,
                                     case when ap.status = 'CLOSED'
                                              then (select date(create_date) from billing.account_period_status_change_hist apsch
                                              where apsch.account_period_id = ap.id and apsch.status = 'CLOSED' order by apsch.create_date desc limit 1) 
                                              end closed_on_date,
                                     ap.id as account_period_id
                                     from billing.account_periods ap) as ap
                         where
                             (coalesce(:status,'0') = '0' or text(ap.status) = :status)
                           and (date(:startDateFrom) is null or date(ap.start_date) >= date(:startDateFrom))
                           and (date(:startDateTo) is null or date(ap.start_date) <= date(:startDateTo))
                           and (date(:endDateFrom) is null or date(ap.end_date) >= date(:endDateFrom))
                           and (date(:endDateTo) is null or date(ap.end_date) <= date(:endDateTo))
                           and (date(:closedOnDateFrom) is null or date(ap.closed_on_date) >= date(:closedOnDateFrom))
                           and (date(:closedOnDateTo) is null or date(ap.closed_on_date) <= date(:closedOnDateTo))
                           and (:prompt is null or (:searchBy = 'ALL' and (
                                 lower(ap.name) like :prompt
                             )
                             )
                             or (
                                    (:searchBy = 'NAME' and lower(ap.name) like :prompt)
                                    )
                             )
                    """, countQuery = """
             select count(1)  from(
                                             select
                                                 ap.name,
                                                 ap.start_date,
                                                 ap.end_date,
                                                 ap.status,
                                                 case when ap.status = 'CLOSED'
                                                      then (select date(create_date) from billing.account_period_status_change_hist apsch
                                                      where apsch.account_period_id = ap.id and apsch.status = 'CLOSED' order by apsch.create_date desc limit 1) 
                                                      end closed_on_date,
                                                 ap.id as account_period_id
                                             from billing.account_periods ap) as ap
                                     where
                                         (coalesce(:status,'0') = '0' or text(ap.status) = :status)
                                       and (date(:startDateFrom) is null or date(ap.start_date) >= date(:startDateFrom))
                                       and (date(:startDateTo) is null or date(ap.start_date) <= date(:startDateTo))
                                       and (date(:endDateFrom) is null or date(ap.end_date) >= date(:endDateFrom))
                                       and (date(:endDateTo) is null or date(ap.end_date) <= date(:endDateTo))
                                       and (date(:closedOnDateFrom) is null or date(ap.closed_on_date) >= date(:closedOnDateFrom))
                                       and (date(:closedOnDateTo) is null or date(ap.closed_on_date) <= date(:closedOnDateTo))
                                       and (:prompt is null or (:searchBy = 'ALL' and (
                                             lower(ap.name) like :prompt
                                         )
                                         )
                                         or (
                                                (:searchBy = 'NAME' and lower(ap.name) like :prompt)
                                                )
                                         )
            """

    )
    Page<AccountingPeriodsListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("status") String status,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            @Param("endDateFrom") LocalDate endDateFrom,
            @Param("endDateTo") LocalDate endDateTo,
            @Param("closedOnDateFrom") LocalDate closedOnDateFrom,
            @Param("closedOnDateTo") LocalDate closedOnDateTo,
            @Param("searchBy") String searchBy,
            Pageable pageable
    );

    Optional<AccountingPeriods> findByIdAndStatus(Long accountingPeriodId, AccountingPeriodStatus status);

    Optional<AccountingPeriods> findByIdAndStatusIn(Long accountingPeriodId, List<AccountingPeriodStatus> status);

    @Query(value = """
            select new bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse(ap)
             from AccountingPeriods ap
            where ap.status = 'OPEN'
              and (:accountperiodname is null or lower(ap.name) like :accountperiodname)
            order by ap.createDate desc
            """)
    Page<AccountingPeriodsResponse> getAvailable(@Param("accountperiodname") String accountperiodname, Pageable pageable);

    @Query("""
             select new bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse(ap)
             from AccountingPeriods ap
            where ap.status = 'OPEN'
              and ap.id = :id
            """)
    Optional<AccountingPeriodsResponse> availableIdForBilling(@Param("id") Long id);

    Long countAccountingPeriodsByName(String name);

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(ap.id,ap.name)
            from AccountingPeriods ap
            where ap.name=:name
            and ap.status=:status
             """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(String name, AccountingPeriodStatus status);

    @Query(value = """
            SELECT ap.id
            FROM AccountingPeriods ap
            WHERE CURRENT_DATE BETWEEN ap.startDate AND ap.endDate
            AND ap.status = 'OPEN'
            """)
    Optional<Long> findCurrentMonthsAccountingPeriodId();

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(ap.id,ap.name)
                from AccountingPeriods ap
                where :date between ap.startDate and ap.endDate
                and ap.status='OPEN'
            """)
    Optional<CacheObject> findAccountingPeriodsByDate(LocalDateTime date);

    @Query(nativeQuery = true, value = """
            with main_curr as (select id, alt_ccy_exchange_rate
                               from nomenclature.currencies
                               where main_ccy_start_date <= current_date
                                 and main_ccy = true
                                 and status = 'ACTIVE'
                               order by main_ccy_start_date desc
                               limit 1)
            select date(ap.end_date)                                            as lastDay,
                   ic.name                                                      as incomeAccountName,
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE' then (0 - (case
                                                                                        when isdd.main_currency_id <> mc.id
                                                                                            then isdd.alt_currency_total_amount_without_vat
                                                                                        else isdd.main_currency_total_amount_without_vat end))
                                    else (case
                                              when isdd.main_currency_id <> mc.id
                                                  then isdd.alt_currency_total_amount_without_vat
                                              else isdd.main_currency_total_amount_without_vat end) end, 0)) +
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE' then (case
                                                                                   when misd.value_currency_id <> mc.id
                                                                                       then misd.value * misd.value_currency_exchange_rate
                                                                                   else misd.value end) * (-1)
                                    else (case
                                              when misd.value_currency_id <> mc.id
                                                  then misd.value * misd.value_currency_exchange_rate
                                              else misd.value end) end, 0)) +
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE' then (0 -
                                                                               (case
                                                                                    when mdocnisd.value_currency_id <> mc.id
                                                                                        then
                                                                                        mdocnisd.value * mdocnisd.value_currency_exchange_rate
                                                                                    else mdocnisd.value end))
                                    else (case
                                              when mdocnisd.value_currency_id <> mc.id
                                                  then
                                                  mdocnisd.value * mdocnisd.value_currency_exchange_rate
                                              else mdocnisd.value end) end, 0)) +
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE' then (case
                                                                                   when idd.measure_unit_for_value_go <> mc.id
                                                                                       then idd.value * mc.alt_ccy_exchange_rate
                                                                                   else idd.value end) * (-1)
                                    else (case
                                              when idd.measure_unit_for_value_go <> mc.id
                                                  then idd.value * mc.alt_ccy_exchange_rate
                                              else idd.value end) end, 0)) +
                   sum(case
                           when b.id is not null then (case
                                                           when mc.id <> i.currency_id
                                                               then i.total_amount_excluding_vat * mc.alt_ccy_exchange_rate
                                                           else i.total_amount_excluding_vat end)
                           else 0 end) +
                   sum(case
                           when i.type = 'REVERSAL' and coalesce(isdd.id, idd.id, mdocnisd.id, misd.id, 0) = 0 then (case
                                                                                                                         when i.document_type = 'CREDIT_NOTE'
                                                                                                                             then
                                                                                                                             (case
                                                                                                                                  when mc.id <> i.currency_id
                                                                                                                                      then i.total_amount_excluding_vat * mc.alt_ccy_exchange_rate
                                                                                                                                  else i.total_amount_excluding_vat end) *
                                                                                                                             (-1)
                                                                                                                         else (case
                                                                                                                                   when mc.id <> i.currency_id
                                                                                                                                       then i.total_amount_excluding_vat * mc.alt_ccy_exchange_rate
                                                                                                                                   else i.total_amount_excluding_vat end) end)
                           else
                               0 end)
                                                                                as totalAmount,
                   (coalesce(sum(isdd.total_volumes), 0) + coalesce(sum(misd.total_volumes), 0)
                       + coalesce(sum(mdocnisd.total_volumes), 0) + coalesce(sum(idd.total_volumes), 0)) /
                   1000                                                         as totalVolumes,
                   coalesce(isdd.income_account_number, misd.income_account_number, mdocnisd.income_account_number,
                            idd.income_account_number, i.income_account_number) as incomeAccountNumber,
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE' then
                                        ((case
                                              when isdd.main_currency_id <> mc.id
                                                  then isdd.alt_currency_total_amount_without_vat
                                              else isdd.main_currency_total_amount_without_vat end) * isdd.vat_rate_percent) /
                                        100 *
                                        (-1)
                                    else ((case
                                               when isdd.main_currency_id <> mc.id
                                                   then isdd.alt_currency_total_amount_without_vat
                                               else isdd.main_currency_total_amount_without_vat end) *
                                          isdd.vat_rate_percent) /
                                         100 end, 0)) +
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE'
                                        then ((case
                                                   when mdocnisd.value_currency_id <> mc.id
                                                       then
                                                       mdocnisd.value * mdocnisd.value_currency_exchange_rate
                                                   else mdocnisd.value end) * mdocnisd.vat_rate_percent) / 100 * (-1)
                                    else ((case
                                               when mdocnisd.value_currency_id <> mc.id
                                                   then
                                                   mdocnisd.value * mdocnisd.value_currency_exchange_rate
                                               else mdocnisd.value end) * mdocnisd.vat_rate_percent) / 100 end, 0)) +
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE' then (case
                                                                                   when misd.value_currency_id <> mc.id
                                                                                       then misd.value * misd.value_currency_exchange_rate
                                                                                   else misd.value end) * misd.vat_rate_percent /
                                                                              100 * (-1)
                                    else
                                        (case
                                             when misd.value_currency_id <> mc.id
                                                 then misd.value * misd.value_currency_exchange_rate
                                             else misd.value end) * misd.vat_rate_percent / 100 end, 0)) +
                   sum(coalesce(case
                                    when i.document_type = 'CREDIT_NOTE' then (case
                                                                                   when idd.measure_unit_for_value_go <> mc.id
                                                                                       then idd.value * mc.alt_ccy_exchange_rate
                                                                                   else idd.value end) * idd.vat_rate_percent /
                                                                              100 * (-1)
                                    else
                                        (case
                                             when idd.measure_unit_for_value_go <> mc.id
                                                 then idd.value * mc.alt_ccy_exchange_rate
                                             else idd.value end) * idd.vat_rate_percent / 100 end, 0)) +
                   sum(case
                           when b.id is not null then (case
                                                           when mc.id <> i.currency_id
                                                               then i.total_amount_of_vat * mc.alt_ccy_exchange_rate
                                                           else i.total_amount_of_vat end)
                           else 0 end) +
                   sum(case
                           when i.type = 'REVERSAL' and coalesce(isdd.id, idd.id, mdocnisd.id, misd.id, 0) = 0 then (case
                                                                                                                         when i.document_type = 'CREDIT_NOTE'
                                                                                                                             then
                                                                                                                             (case
                                                                                                                                  when mc.id <> i.currency_id
                                                                                                                                      then i.total_amount_of_vat * mc.alt_ccy_exchange_rate
                                                                                                                                  else i.total_amount_of_vat end) *
                                                                                                                             (-1)
                                                                                                                         else (case
                                                                                                                                   when mc.id <> i.currency_id
                                                                                                                                       then i.total_amount_of_vat * mc.alt_ccy_exchange_rate
                                                                                                                                   else i.total_amount_of_vat end) end)
                           else
                               0 end)
                                                                                as totalAmountOfVat
            from billing.account_periods ap
                     join invoice.invoices i on ap.id = i.account_period_id
                     cross join main_curr mc
                     join billing.account_period_status_change_hist h
                          on (ap.id = h.account_period_id and h.status = 'OPEN' and
                              h.create_date = (select max(hist.create_date)
                                               from billing.account_period_status_change_hist hist
                                               where hist.status = 'OPEN'
                                                 and hist.account_period_id = ap.id))
                     left join invoice.invoice_standard_detailed_data isdd on i.id = isdd.invoice_id
                     left join invoice.manual_invoice_summary_data misd on i.id = misd.invoice_id
                     left join invoice.manual_debit_or_credit_note_invoice_summary_data mdocnisd on i.id = mdocnisd.invoice_id
                     left join invoice.invoice_detailed_data idd on i.id = idd.invoice_id
                     left join billing.billings b on i.billing_id = b.id and b.type = 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT'
                     left join nomenclature.income_account ic
                               on ((text(ic.number) =
                                    coalesce(isdd.income_account_number, misd.income_account_number, mdocnisd.income_account_number,
                                             idd.income_account_number, i.income_account_number)) and
                                   ic.status = 'ACTIVE' and
                                   ic.create_date = (select max(innic.create_date)
                                                     from nomenclature.income_account innic
                                                     where (text(innic.number) =
                                                            coalesce(isdd.income_account_number, misd.income_account_number,
                                                                     mdocnisd.income_account_number, idd.income_account_number,
                                                                     i.income_account_number))
                                                       and ic.status = 'ACTIVE'))
            where i.status = 'REAL'
              and ap.id = :id
              and (:regenerate = true
                or i.modify_date >= h.create_date)
            group by ap.end_date, ic.name, incomeAccountNumber
            limit :limit offset :offset
                            """)
    List<AccountPeriodSapResponse> getDataForSap(@Param("id") Long id,
                                                 @Param("regenerate") boolean regenerate,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);


    @Query(nativeQuery = true, value = """
            select count(i.id) > 0
            from billing.account_periods ap
                     join billing.account_period_status_change_hist h
                          on (ap.id = h.account_period_id and h.create_date = (select max(hist.create_date)
                                                                               from billing.account_period_status_change_hist hist
                                                                               where hist.account_period_id = ap.id
                                                                                 and hist.status = 'OPEN')
                              and h.status = 'OPEN')
                     join invoice.invoices i on (ap.id = i.account_period_id)
                     join invoice.invoice_cancelations ic on i.invoice_cancelation_id = ic.id
            where ap.id = :id
              and ic.create_date >= h.create_date
              and exists(select 1
                         from billing.account_period_sap_report rep
                         where rep.account_period_id = ap.id
                           and rep.status = 'ACTIVE')
                """)
    boolean mustRegenerateSAP(Long id);

    @Query(nativeQuery = true, value = """
            select count(i.id) > 0
            from billing.account_periods ap
                     join billing.account_period_status_change_hist h
                          on (ap.id = h.account_period_id and h.create_date = (select max(hist.create_date)
                                                                               from billing.account_period_status_change_hist hist
                                                                               where hist.account_period_id = ap.id
                                                                                 and hist.status = 'OPEN')
                              and h.status = 'OPEN')
                     join invoice.invoices i on (ap.id = i.account_period_id)
                     join invoice.invoice_cancelations ic on i.invoice_cancelation_id = ic.id
            where ap.id = :id
              and ic.create_date >= h.create_date
              and exists(select 1
                         from billing.account_period_vat_dairy_report rep
                         where rep.account_period_id = ap.id
                           and rep.status = 'ACTIVE')
                """)
    boolean mustRegenerateVatDairy(Long id);

    @Query(nativeQuery = true, value = """
            select count(tbl)
            from (select distinct coalesce(isdd.income_account_number, misd.income_account_number, mdocnisd.income_account_number,
                                           idd.income_account_number) as incomeAccountNumber
                  from billing.account_periods ap
                           join invoice.invoices i on ap.id = i.account_period_id
                           join billing.account_period_status_change_hist h
                                on (ap.id = h.account_period_id and h.status = 'OPEN' and
                                    h.create_date = (select max(hist.create_date)
                                                     from billing.account_period_status_change_hist hist
                                                     where hist.status = 'OPEN'
                                                       and hist.account_period_id = ap.id))
                           left join invoice.invoice_standard_detailed_data isdd on i.id = isdd.invoice_id
                           left join invoice.manual_invoice_summary_data misd on i.id = misd.invoice_id
                           left join invoice.manual_debit_or_credit_note_invoice_summary_data mdocnisd on i.id = mdocnisd.invoice_id
                           left join invoice.invoice_detailed_data idd on i.id = idd.invoice_id
                           left join nomenclature.income_account ic
                                     on ((text(ic.number) = coalesce(isdd.income_account_number, misd.income_account_number,
                                                                     mdocnisd.income_account_number, idd.income_account_number)) and
                                         ic.status = 'ACTIVE' and
                                         ic.create_date = (select max(innic.create_date)
                                                           from nomenclature.income_account innic
                                                           where (text(innic.number) =
                                                                  coalesce(isdd.income_account_number, misd.income_account_number,
                                                                           mdocnisd.income_account_number,
                                                                           idd.income_account_number))
                                                             and ic.status = 'ACTIVE'))
                  where i.status = 'REAL'
                    and ap.id = :id
                    and (:regenerate = true
                      or i.modify_date >= h.create_date)
                  group by idd.income_account_number, isdd.income_account_number, misd.income_account_number,
                           mdocnisd.income_account_number) as tbl
            """)
    Long countAllIncomeAccs(Long id, boolean regenerate);

    @Query(nativeQuery = true, value = """
            select coalesce(sum(case when i.status = 'REAL' then 1 else 2 end), 0)
            from invoice.invoices i
                     join billing.account_periods ap on i.account_period_id = ap.id
                     join billing.account_period_status_change_hist h
                          on (ap.id = h.account_period_id and h.status = 'OPEN' and
                              h.create_date = (select max(hist.create_date)
                                               from billing.account_period_status_change_hist hist
                                               where hist.status = 'OPEN'
                                                 and hist.account_period_id = ap.id))
                     join customer.customer_details cd on i.customer_detail_id = cd.id
                     join customer.customers c on cd.customer_id = c.id
                     join company.company_details cmpd on cmpd.start_date = (select max(cp.start_date)
                                                                             from company.company_details cp
                                                                             where cp.start_date <= current_date)
            where ap.id = :id
              and text(i.status) in ('REAL', 'CANCELLED')
              and (:regenerate = true
                or i.modify_date >= h.create_date)
            """)
    Long countAllInvoices(Long id, boolean regenerate);

    @Query(nativeQuery = true, value = """
             with row_number
                      as (select row_number() over (order by tbl.id, case when tbl.invoiceType <> '09' then 0 else 1 end) as row_num,
                                 *
                          from (with main_curr as (select id
                                                   from nomenclature.currencies
                                                   where main_ccy_start_date <= current_date
                                                     and main_ccy = true
                                                     and status = 'ACTIVE'
                                                   order by main_ccy_start_date desc
                                                   limit 1)
                                select i.id                                                                      as id,
                                       rpad(substring(cmpd.vat_number from 1 for 15), 15, ' ')                   as vatNumber,
                                       to_char(i.invoice_date, 'yyyyMM')                                         as issuingInvoice,
                                       case
                                           when i.document_type = 'CREDIT_NOTE' then '03'
                                           when i.document_type = 'DEBIT_NOTE' then '02'
                                           when i.document_type in ('INVOICE', 'PROFORMA_INVOICE') then '01' end as invoiceType,
                                       rpad(substring(i.invoice_number from '-(.*)'), 10, ' ')                   as invoiceNumber,
                                       to_char(i.invoice_date, 'dd/MM/yyyy')                                     as issuingDate,
                                       rpad(substring(c.identifier from 1 for 15), 15, ' ')                      as customerIdentifier,
                                       rpad(substring(cd.name from 1 for 50), 50, ' ')                           as customerName,
                                       rpad(case
                                                when i.product_contract_id is not null then 'Ел. енергия'
                                                when coalesce(i.service_order_id, i.service_contract_id, '0') <> '0'
                                                    then 'Услуги'
                                                when i.goods_order_id is not null then 'Стоки'
                                                else (case
                                                          when b.id is not null then (case
                                                                                          when b.prefix_type = 'PRODUCT'
                                                                                              then 'Ел. енергия'
                                                                                          when b.prefix_type = 'SERVICE'
                                                                                              then 'Услуги'
                                                                                          when b.prefix_type = 'GOODS' then 'Стоки'
                                                                                          else '' end)
                                                          else '' end) end, 30,
                                            ' ')                                                                 as contractOrderType,
                                       rpad(text(case
                                                     when i.document_type = 'CREDIT_NOTE'
                                                         then (case
                                                                   when i.currency_id <> main_curr.id then
                                                                       i.total_amount_excluding_vat_in_other_currency
                                                                   else i.total_amount_excluding_vat end) * (-1)
                                                     else (case
                                                               when i.currency_id <> main_curr.id then
                                                                   i.total_amount_excluding_vat_in_other_currency
                                                               else i.total_amount_excluding_vat end) end), 15,
                                            ' ')                                                                 as totalAmountExcludingVat,
                                       rpad(text(case
                                                     when i.document_type = 'CREDIT_NOTE' then (case
                                                                                                    when i.currency_id <> main_curr.id
                                                                                                        then
                                                                                                        i.total_amount_of_vat_in_other_currency
                                                                                                    else i.total_amount_of_vat end) *
                                                                                               (-1)
                                                     else (case
                                                               when i.currency_id <> main_curr.id then
                                                                   i.total_amount_of_vat_in_other_currency
                                                               else i.total_amount_of_vat end) end), 15,
                                            ' ')                                                                 as totalAmountOfVat,
                                       case
                                           when vr20.id is not null then rpad(to_char(case
                                                                                          when i.document_type = 'CREDIT_NOTE'
                                                                                              then (case
                                                                                                        when i.currency_id <> main_curr.id
                                                                                                            then
                                                                                                            vr20.amount_excluding_vat *
                                                                                                            i.currency_exchange_rate_on_invoice_creation
                                                                                                        else vr20.amount_excluding_vat end) *
                                                                                                   (-1)
                                                                                          else (case
                                                                                                    when i.currency_id <> main_curr.id
                                                                                                        then
                                                                                                        vr20.amount_excluding_vat *
                                                                                                        i.currency_exchange_rate_on_invoice_creation
                                                                                                    else vr20.amount_excluding_vat end) end,
                                                                                      'FM999999999990.00'),
                                                                              15,
                                                                              ' ')
                                           else repeat(' ', 15) end                                              as totalAmountFor20,
                                       case
                                           when vr9.id is not null then rpad(to_char(case
                                                                                         when i.document_type = 'CREDIT_NOTE' then
                                                                                             (case
                                                                                                  when i.currency_id <> main_curr.id
                                                                                                      then
                                                                                                      vr9.amount_excluding_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                  else vr9.amount_excluding_vat end) *
                                                                                             (-1)
                                                                                         else (case
                                                                                                   when i.currency_id <> main_curr.id
                                                                                                       then
                                                                                                       vr9.amount_excluding_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                   else vr9.amount_excluding_vat end) end,
                                                                                     'FM999999999990.00'),
                                                                             15,
                                                                             ' ')
                                           else repeat(' ', 15) end                                              as totalAmountFor9,
                                       case
                                           when vr20.id is not null then rpad(to_char(case
                                                                                          when i.document_type = 'CREDIT_NOTE' then
                                                                                              (case
                                                                                                   when i.currency_id <> main_curr.id
                                                                                                       then
                                                                                                       vr20.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                   else vr20.value_of_vat end) *
                                                                                              (-1)
                                                                                          else (case
                                                                                                    when i.currency_id <> main_curr.id
                                                                                                        then
                                                                                                        vr20.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                    else vr20.value_of_vat end) end,
                                                                                      'FM999999999990.00'),
                                                                              15,
                                                                              ' ')
                                           else repeat(' ', 15) end                                              as valueOfVatFor20,
                                       case
                                           when vr9.id is not null then rpad(to_char(case
                                                                                         when i.document_type = 'CREDIT_NOTE' then
                                                                                             (case
                                                                                                  when i.currency_id <> main_curr.id
                                                                                                      then
                                                                                                      vr9.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                  else vr9.value_of_vat end) *
                                                                                             (-1)
                                                                                         else (case
                                                                                                   when i.currency_id <> main_curr.id
                                                                                                       then
                                                                                                       vr9.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                   else vr9.value_of_vat end) end,
                                                                                     'FM999999999990.00'), 15,
                                                                             ' ')
                                           else repeat(' ', 15) end                                              as valueOfVatFor9
                                from invoice.invoices i
                                         join billing.account_periods ap on i.account_period_id = ap.id
                                         join billing.account_period_status_change_hist h
                                              on (ap.id = h.account_period_id and h.status = 'OPEN' and
                                                  h.create_date = (select max(hist.create_date)
                                                                   from billing.account_period_status_change_hist hist
                                                                   where hist.status = 'OPEN'
                                                                     and hist.account_period_id = ap.id))
                                         join customer.customer_details cd on i.customer_detail_id = cd.id
                                         join customer.customers c on cd.customer_id = c.id
                                         join company.company_details cmpd on cmpd.start_date = (select max(cp.start_date)
                                                                                                 from company.company_details cp
                                                                                                 where cp.start_date <= current_date)
                                         left join invoice.invoice_vat_rate_values vr20
                                                   on (i.id = vr20.invoice_id and vr20.vat_rate_percent = 20)
                                         left join invoice.invoice_vat_rate_values vr9
                                                   on (i.id = vr9.invoice_id and vr9.vat_rate_percent = 9)
                                         left join billing.billings b on b.id = i.billing_id
                                         cross join main_curr main_curr
                                where ap.id = :id
                                  and text(i.status) in ('REAL', 'CANCELLED')
                                  and (:regenerate = true
                                    or i.modify_date >= h.create_date)
                                union
                                select i.id                                                                 as id,
                                       rpad(substring(cmpd.vat_number from 1 for 15), 15, ' ')              as vatNumber,
                                       to_char(i.invoice_date, 'yyyyMM')                                    as issuingInvoice,
                                       '09'                                                                 as invoiceType,
                                       rpad(substring(i.invoice_cancellation_number from '-(.*)'), 10, ' ') as invoiceNumber,--needs to be changed
                                       to_char(i.invoice_date, 'dd/MM/yyyy')                                as issuingDate,--needs to be changed
                                       rpad(substring(c.identifier from 1 for 15), 15, ' ')                 as customerIdentifier,
                                       rpad(substring(cd.name from 1 for 50), 50, ' ')                      as customerName,
                                       rpad(case
                                                when i.product_contract_id is not null then 'Ел. енергия'
                                                when coalesce(i.service_order_id, i.service_contract_id, '0') <> '0'
                                                    then 'Услуги'
                                                when i.goods_order_id is not null then 'Стоки'
                                                else (case
                                                          when b.id is not null then (case
                                                                                          when b.prefix_type = 'PRODUCT'
                                                                                              then 'Ел. енергия'
                                                                                          when b.prefix_type = 'SERVICE'
                                                                                              then 'Услуги'
                                                                                          when b.prefix_type = 'GOODS' then 'Стоки'
                                                                                          else '' end)
                                                          else '' end) end, 30,
                                            ' ')                                                            as contractOrderType,
                                       rpad(text((case
                                                      when i.document_type = 'CREDIT_NOTE'
                                                          then (case
                                                                    when i.currency_id <> main_curr.id then
                                                                        i.total_amount_excluding_vat_in_other_currency
                                                                    else i.total_amount_excluding_vat end) * (-1)
                                                      else (case
                                                                when i.currency_id <> main_curr.id then
                                                                    i.total_amount_excluding_vat_in_other_currency
                                                                else i.total_amount_excluding_vat end) end) * (-1)), 15,
                                            ' ')                                                            as totalAmountExcludingVat,
                                       rpad(text((case
                                                      when i.document_type = 'CREDIT_NOTE' then (case
                                                                                                     when i.currency_id <> main_curr.id
                                                                                                         then
                                                                                                         i.total_amount_of_vat_in_other_currency
                                                                                                     else i.total_amount_of_vat end) *
                                                                                                (-1)
                                                      else (case
                                                                when i.currency_id <> main_curr.id then
                                                                    i.total_amount_of_vat_in_other_currency
                                                                else i.total_amount_of_vat end) end) * (-1)), 15,
                                            ' ')                                                            as totalAmountOfVat,
                                       case
                                           when vr20.id is not null then rpad(to_char((case
                                                                                           when i.document_type = 'CREDIT_NOTE'
                                                                                               then (case
                                                                                                         when i.currency_id <> main_curr.id
                                                                                                             then
                                                                                                             vr20.amount_excluding_vat *
                                                                                                             i.currency_exchange_rate_on_invoice_creation
                                                                                                         else vr20.amount_excluding_vat end) *
                                                                                                    (-1)
                                                                                           else (case
                                                                                                     when i.currency_id <> main_curr.id
                                                                                                         then
                                                                                                         vr20.amount_excluding_vat *
                                                                                                         i.currency_exchange_rate_on_invoice_creation
                                                                                                     else vr20.amount_excluding_vat end) end) *
                                                                                      (-1), 'FM999999999990.00'), 15,
                                                                              ' ')
                                           else repeat(' ', 15) end                                         as totalAmountFor20,
                                       case
                                           when vr9.id is not null then rpad(to_char((case
                                                                                          when i.document_type = 'CREDIT_NOTE' then
                                                                                              (case
                                                                                                   when i.currency_id <> main_curr.id
                                                                                                       then
                                                                                                       vr9.amount_excluding_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                   else vr9.amount_excluding_vat end) *
                                                                                              (-1)
                                                                                          else (case
                                                                                                    when i.currency_id <> main_curr.id
                                                                                                        then
                                                                                                        vr9.amount_excluding_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                    else vr9.amount_excluding_vat end) end) *
                                                                                     (-1), 'FM999999999990.00'), 15,
                                                                             ' ')
                                           else repeat(' ', 15) end                                         as totalAmountFor9,
                                       case
                                           when vr20.id is not null then rpad(to_char((case
                                                                                           when i.document_type = 'CREDIT_NOTE' then
                                                                                               (case
                                                                                                    when i.currency_id <> main_curr.id
                                                                                                        then
                                                                                                        vr20.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                    else vr20.value_of_vat end) *
                                                                                               (-1)
                                                                                           else (case
                                                                                                     when i.currency_id <> main_curr.id
                                                                                                         then
                                                                                                         vr20.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                     else vr20.value_of_vat end) end) *
                                                                                      (-1), 'FM999999999990.00'), 15,
                                                                              ' ')
                                           else repeat(' ', 15) end                                         as valueOfVatFor20,
                                       case
                                           when vr9.id is not null then rpad(to_char((case
                                                                                          when i.document_type = 'CREDIT_NOTE' then
                                                                                              (case
                                                                                                   when i.currency_id <> main_curr.id
                                                                                                       then
                                                                                                       vr9.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                   else vr9.value_of_vat end) *
                                                                                              (-1)
                                                                                          else (case
                                                                                                    when i.currency_id <> main_curr.id
                                                                                                        then
                                                                                                        vr9.value_of_vat * i.currency_exchange_rate_on_invoice_creation
                                                                                                    else vr9.value_of_vat end) end) *
                                                                                     (-1), 'FM999999999990.00'), 15, ' ')
                                           else repeat(' ', 15) end                                         as valueOfVatFor9
                                from invoice.invoices i
                                         join billing.account_periods ap on i.account_period_id = ap.id
                                         join billing.account_period_status_change_hist h
                                              on (ap.id = h.account_period_id and h.status = 'OPEN' and
                                                  h.create_date = (select max(hist.create_date)
                                                                   from billing.account_period_status_change_hist hist
                                                                   where hist.status = 'OPEN'
                                                                     and hist.account_period_id = ap.id))
                                         join customer.customer_details cd on i.customer_detail_id = cd.id
                                         join customer.customers c on cd.customer_id = c.id
                                         join company.company_details cmpd on cmpd.start_date = (select max(cp.start_date)
                                                                                                 from company.company_details cp
                                                                                                 where cp.start_date <= current_date)
                                         left join invoice.invoice_vat_rate_values vr20
                                                   on (i.id = vr20.invoice_id and vr20.vat_rate_percent = 20)
                                         left join invoice.invoice_vat_rate_values vr9
                                                   on (i.id = vr9.invoice_id and vr9.vat_rate_percent = 9)
                                         left join billing.billings b on i.billing_id = b.id
                                         cross join main_curr main_curr
                                where ap.id = :id
                                  and i.status = 'CANCELLED'
                                  and (:regenerate = true
                                    or i.modify_date >= h.create_date)) as tbl)
             select concat(vatnumber,
                           issuinginvoice,
                           repeat(' ', 4),
                           lpad(cast((row_num + :offset) as varchar), 15, '0'),
                           invoicetype,
                           invoicenumber,
                           issuingdate,
                           customeridentifier,
                           customername,
                           contractordertype,
                           totalamountexcludingvat,
                           totalamountofvat,
                           totalamountfor20,
                           valueofvatfor20,
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           totalamountfor9,
                           valueofvatfor9,
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 11),
                           '0.00',
                           repeat(' ', 13)) as value,
                    row_num + :offset       as rowNum
             from row_number
             limit :limit offset :offset
            """)
    List<AccountPeriodVatResponse> getDataForVatDairy(Long id, boolean regenerate, int offset, int limit);

}
