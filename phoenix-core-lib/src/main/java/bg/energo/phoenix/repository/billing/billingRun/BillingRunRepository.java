package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunPeriodicity;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.response.billing.billingRun.*;
import bg.energo.phoenix.service.notifications.interfaces.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRunRepository extends JpaRepository<BillingRun, Long>, Notification {
    @Query(
            """
                    select c from BillingRun as c
                        where c.accountingPeriodId = :id and c.status in(:statuses)
                    """
    )
    Optional<List<BillingRun>> findBillingRunByAccountingPeriod(Long id, List<BillingStatus> statuses);

    @Query("SELECT COUNT(b) FROM BillingRun b WHERE b.createDate >= :startOfDay AND b.createDate < :startOfNextDay")
    Long countBillingRunByCreateDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("startOfNextDay") LocalDateTime startOfNextDay);

    Optional<BillingRun> findBillingRunByIdAndStatusIsNot(Long id, BillingStatus status);

    @Query(nativeQuery = true,
            value = """
                              select
                                      b.billing_number as billingNumber,
                                      text(b.run_stage) as runStage,
                                      b.criteria as billingCriteria,
                                      b.application_level as applicationLevel,
                                      b.type as billingType,
                                      b.status as status,
                                      (select name from billing.account_periods ap where id = b.account_period_id) as accountingPeriod,
                                      b.execution_type as executionType,
                                      b.run_periodicity as runPeriodicity,
                                      b.id as billingId,
                                      b.invoice_due_date as invoiceDueDate,
                                      b.invoice_date as invoiceDate
                             from billing.billings b where (b.run_periodicity =  'STANDARD' or b.run_periodicity is null)
                                      and ((:automation) is null or cast(b.run_stage as text[]) && cast(:automation as text[]))
                                      and ((:executionType) is null or text(b.execution_type)  in :executionType)
                                      and ((:billingtype) is null or text(b.type)  in :billingtype)
                                      and ((:billingcriteria) is null or text(b.criteria)  in :billingcriteria)
                                      and ((:applicationlevel) is null or text(b.application_level)  in :applicationlevel)
                                      and (date(:invoiceDueDateFrom) is null or b.invoice_due_date >= date(:invoiceDueDateFrom))
                                      and (date(:invoiceDueDateTo) is null or b.invoice_due_date <= date(:invoiceDueDateTo))
                                      and (date(:invoiceDateFrom) is null or b.invoice_date >= date(:invoiceDateFrom))
                                      and (date(:invoiceDateTo) is null or b.invoice_date <= date(:invoiceDateTo))
                                      and ((:statuses) is null or text(b.status)  in :statuses)
                                      and (:prompt is null or (:searchBy = 'ALL' and (
                                                                          lower(b.billing_number) like :prompt or
                                                                          lower(text(b.type)) like :prompt or
                                                                          text(b.invoice_due_date) like :prompt or
                                                                          lower(text(b.status))  like :prompt or
                                                                          lower(text(b.execution_type))  like :prompt
                                                                )
                                                              )
                                                          or (
                                                              (:searchBy = 'BILLINGNUMBER' and lower(b.billing_number) like :prompt) or
                                                              (:searchBy = 'BILLINGTYPE' and lower(text(b.type)) like :prompt) or
                                                              (:searchBy = 'INVOICEDUEDATE' and text(b.invoice_due_date) like :prompt) or
                                                              (:searchBy = 'STATUS' and lower(text(b.status))  like :prompt) or
                                                              (:searchBy = 'PERFORMANCETYPE' and lower(text(b.execution_type)) like :prompt)                                
                                                          )
                                      )
                    """, countQuery = """
                             select count(1)
                                      from billing.billings b where (b.run_periodicity =  'STANDARD' or b.run_periodicity is null)
                                      and ((:automation) is null or cast(b.run_stage as text[]) && cast(:automation as text[]))
                                      and ((:executionType) is null or text(b.execution_type)  in :executionType)
                                      and ((:billingtype) is null or text(b.type)  in :billingtype)
                                      and ((:billingcriteria) is null or text(b.criteria)  in :billingcriteria)
                                      and ((:applicationlevel) is null or text(b.application_level)  in :applicationlevel)
                                      and (date(:invoiceDueDateFrom) is null or b.invoice_due_date >= date(:invoiceDueDateFrom))
                                      and (date(:invoiceDueDateTo) is null or b.invoice_due_date <= date(:invoiceDueDateTo))
                                      and (date(:invoiceDateFrom) is null or b.invoice_date >= date(:invoiceDateFrom))
                                      and (date(:invoiceDateTo) is null or b.invoice_date <= date(:invoiceDateTo))
                                      and ((:statuses) is null or text(b.status)  in :statuses)
                                      and (:prompt is null or (:searchBy = 'ALL' and (
                                                                          lower(b.billing_number) like :prompt or
                                                                          lower(text(b.type)) like :prompt or
                                                                          text(b.invoice_due_date) like :prompt or
                                                                          lower(text(b.status))  like :prompt or
                                                                          lower(text(b.execution_type))  like :prompt
                                                                )
                                                              )
                                                          or (
                                                              (:searchBy = 'BILLINGNUMBER' and lower(b.billing_number) like :prompt) or
                                                              (:searchBy = 'BILLINGTYPE' and lower(text(b.type)) like :prompt) or
                                                              (:searchBy = 'INVOICEDUEDATE' and text(b.invoice_due_date) like :prompt) or
                                                              (:searchBy = 'STATUS' and lower(text(b.status))  like :prompt) or
                                                              (:searchBy = 'PERFORMANCETYPE' and lower(text(b.execution_type)) like :prompt)                               
                                                          )
                                      )
            """
    )
    Page<BillingRunListingMiddleResponse> filter(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("automation") String runStages,
            @Param("executionType") List<String> executionType,
            @Param("billingtype") List<String> billingTypes,
            @Param("billingcriteria") List<String> billingCriteria,
            @Param("applicationlevel") List<String> applicationLevel,
            @Param("invoiceDueDateFrom") LocalDate invoiceDueDateFrom,
            @Param("invoiceDueDateTo") LocalDate invoiceDueDateTo,
            @Param("invoiceDateFrom") LocalDate invoiceDateFrom,
            @Param("invoiceDateTo") LocalDate invoiceDateTo,
            @Param("statuses") List<String> billingStatuses,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    select
                    b.billing_number as billingNumber,
                    b.run_stage as runStages,
                    b.criteria as billingCriteria,
                    b.application_level as applicationLevel,
                    b.process_periodicity as processPeriodicity,
                    b.id as billingId,
                    b.type as type
                    from
                    (
                    select
                    b.billing_number as billing_number,
                     text(b.run_stage) as run_stage,
                     b.criteria as criteria,
                     b.type as type,
                     b.application_level as application_level,
                    (case when :processperiodicityDirection = 'ASC' then vbpp.process_periodicity_name when :processperiodicityDirection = 'DESC' then vbpp.process_periodicity_name_desc else vbpp.process_periodicity_name end ) as process_periodicity,
                    b.id as id
                    from
                    billing.billings b
                    left join
                    billing.vw_billing_process_periodicity vbpp
                      on vbpp.billing_id = b.id
                    where b.run_periodicity =  'PERIODIC'
                    and ((:automation) is null or cast(b.run_stage as text[]) && cast(:automation as text[]))
                     and ((:billingcriteria) is null or text(b.criteria)  in :billingcriteria)
                     and ((:applicationlevel) is null or text(b.application_level)  in :applicationlevel)
                    and ((:billingType) is null or text(b.type)  in :billingType)
                     and ((:processperiodicity) is null or exists(select 1 from
                                                                             billing.billing_process_periodicity bpp
                                                                              where bpp.billing_id = b.id
                                                                                and bpp.status = 'ACTIVE'
                                                                                and bpp.process_periodicity_id in (:processperiodicity)))
                     and (:prompt is null or (:searchBy = 'ALL' and (
                                                         lower(b.billing_number) like :prompt
                                                          or
                                                         lower(text(b.application_level)) like :prompt
                                                          or
                                                         lower(text(b.criteria))  like :prompt
                                                          or exists(select 1 from
                                                                             billing.billing_process_periodicity bpp
                                                                               join billing.process_periodicity pp
                                                                                 on bpp.process_periodicity_id = pp.id
                                                                              where bpp.billing_id = b.id
                                                                                and bpp.status = 'ACTIVE'
                                                                                and pp.status = 'ACTIVE'
                                                                                and lower(pp.name) like  :prompt)
                                                     )
                                                 )
                                                 or (
                                                     (:searchBy = 'BILLINGNUMBER' and lower(b.billing_number) like :prompt)
                                                      or
                                                     (:searchBy = 'APPLICATIONLEVEL' and lower(text(b.application_level)) like :prompt)
                                                      or
                                                     (:searchBy = 'BILLINFCRITERIA' and lower(text(b.criteria)) like :prompt)
                                                      or
                                                     (:searchBy = 'PROCESSPERIODICITYNAME' and exists(select 1 from
                                                                             billing.billing_process_periodicity bpp
                                                                               join billing.process_periodicity pp
                                                                                 on bpp.process_periodicity_id = pp.id
                                                                              where bpp.billing_id = b.id
                                                                                and bpp.status = 'ACTIVE'
                                                                                and pp.status = 'ACTIVE'
                                                                                and lower(pp.name) like  :prompt))
                                                      )
                                             )) as b
                                """,
            countQuery = """ 
                    select
                    count (*)from
                    (
                    select
                    b.billing_number as billing_number,
                     text(b.run_stage) as run_stage,
                     b.criteria as criteria,
                     b.application_level as application_level,
                    (case when :processperiodicityDirection = 'ASC' then vbpp.process_periodicity_name when :processperiodicityDirection = 'DESC' then vbpp.process_periodicity_name_desc else vbpp.process_periodicity_name end ) as process_periodicity,
                    b.id as id
                    from
                    billing.billings b
                    left join
                    billing.vw_billing_process_periodicity vbpp
                      on vbpp.billing_id = b.id
                    where b.run_periodicity =  'PERIODIC'
                    and ((:automation) is null or cast(b.run_stage as text[]) && cast(:automation as text[]))
                     and ((:billingcriteria) is null or text(b.criteria)  in :billingcriteria)
                     and ((:applicationlevel) is null or text(b.application_level)  in :applicationlevel)
                     and ((:billingType) is null or text(b.type)  in :billingType)
                     and ((:processperiodicity) is null or exists(select 1 from
                                                                             billing.billing_process_periodicity bpp
                                                                              where bpp.billing_id = b.id
                                                                                and bpp.status = 'ACTIVE'
                                                                                and bpp.process_periodicity_id in ((:processperiodicity))))
                     and (:prompt is null or (:searchBy = 'ALL' and (
                                                         lower(b.billing_number) like :prompt
                                                          or
                                                         lower(text(b.application_level)) like :prompt
                                                          or
                                                         lower(text(b.criteria))  like :prompt
                                                          or exists(select 1 from
                                                                             billing.billing_process_periodicity bpp
                                                                               join billing.process_periodicity pp
                                                                                 on bpp.process_periodicity_id = pp.id
                                                                              where bpp.billing_id = b.id
                                                                                and bpp.status = 'ACTIVE'
                                                                                and pp.status = 'ACTIVE'
                                                                                and lower(pp.name) like  :prompt)
                                                     )
                                                 )
                                                 or (
                                                     (:searchBy = 'BILLINGNUMBER' and lower(b.billing_number) like :prompt)
                                                      or
                                                     (:searchBy = 'APPLICATIONLEVEL' and lower(text(b.application_level)) like :prompt)
                                                      or
                                                     (:searchBy = 'BILLINFCRITERIA' and lower(text(b.criteria)) like :prompt)
                                                      or
                                                     (:searchBy = 'PROCESSPERIODICITYNAME' and exists(select 1 from
                                                                             billing.billing_process_periodicity bpp
                                                                               join billing.process_periodicity pp
                                                                                 on bpp.process_periodicity_id = pp.id
                                                                              where bpp.billing_id = b.id
                                                                                and bpp.status = 'ACTIVE'
                                                                                and pp.status = 'ACTIVE'
                                                                                and lower(pp.name) like  :prompt))
                                                      )
                                             )) as b
                                             """)
    Page<BillingRunListingPeriodicMiddleResponse> filterPeriodic(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("automation") String runStages,
            @Param("billingcriteria") List<String> billingCriteria,
            @Param("applicationlevel") List<String> applicationLevel,
            @Param("billingType") List<String> billingType,
            @Param("processperiodicity") List<Long> processPeriodicity,
            @Param("processperiodicityDirection") String processPeriodicityDirection,
            Pageable pageable
    );

    Optional<BillingRun> findBillingRunByIdAndRunPeriodicityAndStatusIsNot(Long id, BillingRunPeriodicity billingRunPeriodicity, BillingStatus status);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.billing.billingRun.BillingRunFilterByResponse(b.id,b.billingNumber)
                    from BillingRun b
                    where b.status <> 'DELETED'
                    and (coalesce(:billingnumber,'0') = '0' or  lower(b.billingNumber) like :billingnumber)
                    """
    )
    Page<BillingRunFilterByResponse> filterByBillingNumber(
            @Param("billingnumber") String billingnumber,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    select invoice_tbl.id                     as id,
                           invoice_tbl.invoicenumber          as invoiceNumber,
                           invoice_tbl.invoicedocumenttype    as invoiceDocumenttype,
                           invoice_tbl.invoicetype            as invoiceType,
                           invoice_tbl.invoicedate            as invoiceDate,
                           invoice_tbl.customer               as customer,
                           invoice_tbl.accountingperiod       as accountingPeriod,
                           invoice_tbl.billingrun             as billingRun,
                           invoice_tbl.basisforissuing        as basisForIssuing,
                           invoice_tbl.meterreadingperiodfrom as meterReadingPeriodFrom,
                           invoice_tbl.meterreadingperiodto   as meterReadingPeriodTo,
                           invoice_tbl.totalamount            as totalAmount,
                           invoice_tbl.isMarkedAsRemoved      as isMarkedAsRemoved
                    from (select i.id                         as id,
                                 i.invoice_number             as invoicenumber,
                                 i.document_type              as invoicedocumenttype,
                                 i.type                       as invoicetype,
                                 i.invoice_date               as invoicedate,
                                 (
                                     case
                                         when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier, ' (', cd.name, ' ', lf.name, ')')
                                         else concat(c.identifier, ' (', cd.name, ' ',
                                                     case when cd.middle_name is not null then concat(cd.middle_name, ' ') else '' end,
                                                     cd.last_name, ')')
                                         end
                                     )                        as customer,
                                 ap.name                      as accountingperiod,
                                 b.billing_number             as billingrun,
                                 i.basis_for_issuing          as basisforissuing,
                                 i.meter_reading_period_from  as meterreadingperiodfrom,
                                 i.meter_reading_period_to    as meterreadingperiodto,
                                 i.total_amount_including_vat as totalamount,
                                 (case when :draftsTab then (brdim2 is not null) else (brdim is not null) end)         as isMarkedAsRemoved,
                                 b.status as billingStatus
                          from invoice.invoices i
                                   join billing.billings b on b.id = i.billing_id
                                   left join billing.account_periods ap on b.account_period_id = ap.id
                                   left join customer.customer_details cd on cd.id = i.customer_detail_id
                                   left join customer.customers c on cd.customer_id = c.id
                                   left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                                   left join billing.billing_run_draft_pdf_invoices_marks brdim
                                             on (brdim.billing_run_id = b.id and brdim.invoice_id = i.id)
                                    left join billing.billing_run_draft_invoices_marks brdim2
                                             on (brdim2.billing_run_id = b.id and brdim2.invoice_id = i.id)
                          where b.id = :billingId
                            and (case when :draftsTab then text(i.status) = 'DRAFT' else text(i.status) = 'DRAFT_GENERATED' end)
                            and (:prompt is null or lower(coalesce(i.invoice_number, '')) like :prompt))
                             as invoice_tbl
                            where case when invoice_tbl.billingStatus = 'COMPLETED'
                                   then  isMarkedAsRemoved = false
                                 else isMarkedAsRemoved = false or isMarkedAsRemoved = true
                                end
                    """,
            countQuery = """
                    select count(invoice_tbl.id)
                    from (select i.id                         as id,
                         (case when :draftsTab then (brdim2 is not null) else (brdim is not null) end)    as isMarkedAsRemoved,
                          b.status as billingStatus
                          from invoice.invoices i
                                   join billing.billings b on b.id = i.billing_id
                                   left join billing.account_periods ap on b.account_period_id = ap.id
                                   left join customer.customer_details cd on cd.id = i.customer_detail_id
                                   left join customer.customers c on cd.customer_id = c.id
                                   left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                                   left join billing.billing_run_draft_invoices_marks brdim
                                             on (brdim.billing_run_id = b.id and brdim.invoice_id = i.id)
                                   left join billing.billing_run_draft_invoices_marks brdim2
                                             on (brdim2.billing_run_id = b.id and brdim2.invoice_id = i.id)
                          where b.id = :billingId
                            and (case when :draftsTab then text(i.status) = 'DRAFT' else text(i.status) = 'DRAFT_GENERATED' end)
                            and (:prompt is null or lower(coalesce(i.invoice_number, '')) like :prompt))
                             as invoice_tbl
                            where case when invoice_tbl.billingStatus = 'COMPLETED'
                                   then  isMarkedAsRemoved = false
                                 else isMarkedAsRemoved = false or isMarkedAsRemoved = true
                                end
                    """)
    Page<BillingRunInvoiceViewResponse> getInvoicesForBillingRun(@Param("draftsTab") Boolean draftsTab,
                                                                 @Param("billingId") Long billingId,
                                                                 @Param("prompt") String prompt,
                                                                 Pageable pageable);


    @Query(nativeQuery = true, value = """
                select   b.id AS id,
                         b.billing_number AS billingNumber,
                         b.run_periodicity AS runPeriodicity,
                         b.additional_info AS additionalInfo,
                         b.type AS type,
                         b.status AS status,
                         b.tax_event_date AS taxEventDate,
                         b.invoice_date AS invoiceDate,
                         b.account_period_id AS accountingPeriodId,
                         b.invoice_due_date_type AS invoiceDueDateType,
                         b.invoice_due_date AS invoiceDueDate,
                         b.sending_an_invoice AS sendingAnInvoice,
                         b.execution_type AS executionType,
                         b.execution_date AS executionDate,
                         b.criteria AS billingCriteria,
                         b.application_level AS applicationLevel,
                         b.customer_contract_or_pod_conditions AS customerContractOrPodConditions,
                         b.customer_contract_or_pod_list AS customerContractOrPodList,
                         b.vat_rate_id AS vatRateId,
                         b.global_vat_rate AS globalVatRate,
                         b.applicable_interest_rate_id AS interestRateId,
                         b.bank_id AS bankId,
                         b.iban AS iban,
                         b.amount_excluding_vat AS amountExcludingVat,
                         b.issuing_for_month_to_current AS issuingForTheMonthToCurrent,
                         b.amount_excluding_vat_currency_id AS currencyId,
                         b.deduction_from AS deductionFrom,
                         b.income_account_number AS numberOfIncomeAccount,
                         b.cost_center_controlling_order AS costCenterControllingOrder,
                         b.customer_detail_id AS customerDetailId,
                         b.product_contract_id AS productContractId,
                         b.service_contract_id AS serviceContractId,
                         b.document_type AS documentType,
                         b.goods_order_id AS goodsOrderId,
                         b.service_order_id AS serviceOrderId,
                         b.basic_for_issuing AS basisForIssuing,
                         b.manual_invoice_type AS manualInvoiceType,
                         b.direct_debit AS directDebit,
                         b.customer_communication_id AS customerCommunicationId,
                         b.invoice_list AS listOfInvoices,
                         b.price_change AS priceChange,
                         b.periodicity_created_from_id AS periodicityCreatedFromId,
                         b.employee_id AS employeeId,
                         text(b.application_model_type) AS applicationModelType,
                         text(b.run_stage) AS runStages,
                         text(b.invoice_type ) AS issuedSeparateInvoices,
                         bpp.create_date as periodicityAddDate,
                       pp.id as periodicityId,
                       pp.billing_process_start as processExecutionType,
                       pp.billing_process_start_date as processxecutionDate,
                       (select ppti.start_time from billing.process_periodicity_time_intervals ppti where ppti.process_periodicity_id=pp.id order by ppti.start_time limit 1) as startTime,
                       b.periodic_max_end_date  as maxEndDate,
                       b.periodic_max_end_date_value  as maxEndDateValue
                       from billing.billings b
                join billing.billing_process_periodicity bpp on bpp.billing_id=b.id
                join billing.process_periodicity pp on pp.id=bpp.process_periodicity_id
                where bpp.status='ACTIVE' and b.run_periodicity='PERIODIC'
                  and (pp.type='PERIODICAL' and not exists (select 1 from billing.billings b2 where b2.periodicity_created_from_id=pp.id 
                                                                                                and b2.periodic_billing_created_from=b.id
                                                                                                and (b2.run_periodicity is null or b2.run_periodicity='STANDARD')
                                                                                                and date(b2.create_date)=date(current_date))or
                       (pp.type='ONE_TIME' and not exists (select 1 from billing.billings b2 where b2.periodicity_created_from_id=pp.id 
                                                                                               and b2.periodic_billing_created_from=b.id
                                                                                               and (b2.run_periodicity is null or b2.run_periodicity='STANDARD'))))
                and ( (pp.type = 'PERIODICAL' and
                       
                       (
                            (pp.calendar_id is null and current_date = (
                    WITH closest_future_date AS (
                        select s from billing.check_process_periodicity(pp.id,current_date,current_date + interval '365 day') s
                    )
                        (select s from closest_future_date cfd)
                    ))
                        or 
                           (pp.change_to is null and current_date=(
                               WITH closest_future_date AS (
                                   select s from billing.check_process_periodicity(pp.id,current_date,current_date + interval '365 day') s
                               )
                               (select s from closest_future_date cfd)
                               ) and current_date in (select date from billing.get_working_days(pp.id,date(current_date), date(current_date)) as date) )
                               or
                
                           (pp.change_to = 'PREVIOUS_WORKING_DAY' and (current_date = (
                               WITH closest_future_date AS (
                                   select s from billing.check_process_periodicity(pp.id,current_date,current_date + interval '365 day') s
                               )
                                    SELECT ( date)
                               FROM (
                                        -- Your select query here
                                        select date from billing.get_working_days(pp.id,date(current_date), date((select s from closest_future_date cfd))) as date
                                    ) AS subquery
                               ORDER BY ABS(date((select s from closest_future_date cfd)) - date)
                               LIMIT 1
                               )) ) or
                           (pp.change_to = 'NEXT_WORKING_DAY'
                               and current_date= (
                                   WITH closest_future_date AS (
                                       select s from billing.check_process_periodicity_inverted(pp.id,current_date- interval '365 day',current_date ) s
                                   )
                
                                   SELECT ( date)
                                   FROM (
                                            -- Your select query here
                                            select date from billing.get_working_days(pp.id, date((select s from closest_future_date cfd)),date(current_date)) as date
                                        ) AS subquery
                                   ORDER BY ABS(date((select s from closest_future_date cfd)) - date)
                                   LIMIT 1
                                   )
                               )
                
                           )) or (pp.type='ONE_TIME' and pp.billing_process_start='DATE_AND_TIME' and date(pp.billing_process_start_date)=date(current_date)))
                order by periodicityAddDate desc
            """)
    List<OneTimeCreationModel> findAllPeriodicForOneTimeCreation();


    @Query(value = """
            select b.id     as id,
                   b.status as status
            from billing.billings b
                     left join billing.process_periodicity pp
                               on pp.id = b.periodicity_created_from_id
                                   and pp.status = 'ACTIVE'
            where (b.run_periodicity = 'STANDARD' or b.run_periodicity is null)
              and b.execution_type = 'EXACT_DATE'
              and (b.periodicity_created_from_id is null or not exists(select 1
                                                                       from billing.process_periodicity_incompatible_billings ppib
                                                                                join billing.billings incomp_b on ppib.incompatible_billing_id = incomp_b.id
                                                                                join billing.billing_process_periodicity bpp
                                                                                     on (incomp_b.id = bpp.billing_id and bpp.status = 'ACTIVE')
                                                                                join billing.process_periodicity pp2 on bpp.process_periodicity_id = pp2.id
                                                                                join billing.billings one_time_billing
                                                                                     on (one_time_billing.periodicity_created_from_id =
                                                                                         pp2.id and
                                                                                         one_time_billing.periodic_billing_created_from =
                                                                                         incomp_b.id)
                                                                       where ppib.process_periodicity_id = pp.id
                                                                         and text(one_time_billing.status) in
                                                                             ('IN_PROGRESS_DRAFT', 'IN_PROGRESS_GENERATION',
                                                                              'IN_PROGRESS_ACCOUNTING')
                                                                         and ppib.status = 'ACTIVE'))
              and (
                (b.status = 'INITIAL'
                    and current_timestamp >= b.execution_date
                    )
                    or
                (b.status = 'PAUSED'
                    and
                 (b.periodicity_created_from_id is not null and exists(select 1
                                                                       from billing.process_periodicity_time_intervals ppti
                                                                       where ppti.process_periodicity_id = pp.id
                                                                         and ppti.status = 'ACTIVE'
                                                                         and ppti.end_time is not null
                                                                         and current_time between cast(ppti.start_time as time) and cast(ppti.end_time as time))
                     or current_time > cast((select coalesce(max(ppti.end_time), max(ppti.start_time))
                                             from billing.process_periodicity_time_intervals ppti
                                             where ppti.process_periodicity_id = pp.id
                                               and ppti.status = 'ACTIVE') as time)
                     )
                    )
                    or (text(b.status) in ('IN_PROGRESS_DRAFT', 'IN_PROGRESS_GENERATION', 'IN_PROGRESS_ACCOUNTING')
                    and (b.periodicity_created_from_id is not null and
                         not exists(select 1
                                    from billing.process_periodicity_time_intervals ppti
                                    where ppti.process_periodicity_id = pp.id
                                      and ppti.end_time is not null
                                      and current_time between cast(ppti.start_time as time) and cast(ppti.end_time as time))
                        and (current_time between cast((select min(ppti.start_time)
                                                        from billing.process_periodicity_time_intervals ppti
                                                        where ppti.process_periodicity_id = pp.id
                                                          and ppti.status = 'ACTIVE') as time)
                            and cast((select case
                                                 when max(ppti.end_time) is not null
                                                     then greatest(max(ppti.end_time), max(ppti.start_time))
                                                 else max(ppti.start_time) end

                                      from billing.process_periodicity_time_intervals ppti
                                      where ppti.process_periodicity_id = pp.id
                                        and ppti.status = 'ACTIVE') as time))
                            )
                    )
                )
              and (case
                       when b.periodicity_created_from_id is not null and pp.start_after_process_billing_id is not null
                           then (exists(select 1
                                        from billing.billings after_process_b
                                                 join billing.billing_process_periodicity bpp
                                                      on (after_process_b.id = bpp.billing_id and bpp.status = 'ACTIVE')
                                                 join billing.process_periodicity pp3 on bpp.process_periodicity_id = pp3.id
                                                 join billing.billings one_times
                                                      on (one_times.periodicity_created_from_id = pp3.id
                                                          and one_times.periodic_billing_created_from = after_process_b.id)
                                        where after_process_b.id = pp.start_after_process_billing_id)
                           and
                                 not exists(select after_process_b
                                            from billing.billings after_process_b
                                                     join billing.billing_process_periodicity bpp
                                                          on (after_process_b.id = bpp.billing_id and bpp.status = 'ACTIVE')
                                                     join billing.process_periodicity pp3 on bpp.process_periodicity_id = pp3.id
                                                     join billing.billings one_times
                                                          on (one_times.periodicity_created_from_id = pp3.id
                                                              and one_times.periodic_billing_created_from = after_process_b.id)
                                            where after_process_b.id = pp.start_after_process_billing_id
                                              and case
                                                      when ((one_times.execution_type = 'EXACT_DATE'
                                                          and date(one_times.execution_date) = current_date)
                                                          or (one_times.execution_type in ('IMMEDIATELY', 'MANUAL')
                                                              and date(one_times.create_date) = current_date))
                                                          then one_times.status <> 'COMPLETED'
                                                      else false end))
                       else true end)
            """, nativeQuery = true)
    List<BillingWithStatusShortResponse> getOneTimeBillingsFromPeriodicity();

    @Query("select b.status='PAUSED' from BillingRun b where b.id=:billingId")
    boolean isBillingRunPaused(Long billingId);

    @Query(value = """
            select  b.invoice_date as invoiceDate,
                    b.tax_event_date as taxEventDate,
                    b.invoice_due_date_type as invoiceDueDateType,
                    b.invoice_due_date as invoiceDueDate,
                    text(b.application_model_type) as applicationModelType,
                    b.account_period_id as accountPeriodId
                from billing.billings b
                where b.id = :id
            """, nativeQuery = true)
    Optional<BillingRunStandardProcessModel> findByIdForStandardProcess(Long id);

    @Query(value = """
                    select run.*
                    from billing.billings run
                    where (
                        (
                            text(run.run_main_data_preparation_status) = 'FINISHED'
                        )
                    ) and run.status = 'IN_PROGRESS_DRAFT'
            """, nativeQuery = true)
    List<BillingRun> findBillingRunsWithPreparedStandardBillingRunData();

    @Query(value = """
                    select distinct run.*
                    from billing.billings run
                    where status = 'IN_PROGRESS_DRAFT'
                      and ((text(application_model_type) not like ('%INTERIM_AND_ADVANCE_PAYMENT%')
                        and run.run_main_inovice_generation_status = 'FINISHED')
                        or (text(application_model_type) like ('%INTERIM_AND_ADVANCE_PAYMENT%')
                            and run.run_interim_invoice_generation_status = 'FINISHED'))
            """, nativeQuery = true)
    List<BillingRun> findStandardBillingRunWithFinishedBillingData();

    @Query(value = """
            select distinct run
            from BillingRun run
            where (
                run.mainInvoiceGenerationStatus = 'RUNNING'
                or run.interimInvoiceGenerationStatus = 'RUNNING'
            )
            and text(run.status) = 'IN_PROGRESS_DRAFT'
            """)
    List<BillingRun> findNonFinishedProcesses();

    @Query("""
            select case when b.type = 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT' then true else false end
            from BillingRun b
            join Invoice i on i.billingId = b.id
            where i.id = :id
            """)
    boolean isBillingRunTypeManualInterim(Long id);

    @Query("""
            select run.status
            from BillingRun run
            where run.id = :runId
            """)
    BillingStatus getBillingRunCurrentStatusById(@Param("runId") Long runId);

    @Transactional
    @Modifying
    @Query(value = """
            call billing_run.finalize_data_generation(:billingRunId)
            """, nativeQuery = true)
    void finalizeDataPreparation(@Param("billingRunId") Long billingRunId);

    @Query(value = """
            select cam.id
            from billing.billings run
                     join billing.billing_notifications bn on bn.billing_id = run.id
                     join customer.account_managers cam on cam.id = bn.employee_id
            where run.id = :runId
              and text(bn.notification_type) = :notificationState
            union
            select camt.account_manager_id
            from billing.billings run
                     join billing.billing_notifications bn on bn.billing_id = run.id
                     join customer.portal_tags pt on pt.id = bn.tag_id
                     join customer.account_manager_tags camt on camt.portal_tag_id = pt.id
            where run.id = :runId
              and text(bn.notification_type) = :notificationState
            """, nativeQuery = true)
    List<Long> getNotificationTargets(Long runId, String notificationState);

    List<BillingRun> findBillingRunByStatus(BillingStatus status);

    @Query("""
            select count(1)>0 from BillingRun b
            join Invoice inv on inv.billingId=b.id
            join ContractBillingGroup cbg on cbg.id=inv.contractBillingGroupId
            where b.id=:id
            and inv.invoiceStatus='REAL'
            """)
    boolean billingContainsOnPaperFlow(Long id);

    @Query("""
            select count(i.id) > 0
            from Invoice i
            join BillingRunInvoices bi on bi.invoiceId = i.id
            where bi.billingId = :billingRunId
            and i.invoiceStatus = 'CANCELLED'
            """)
    boolean hasAnyCancelledInvoiceConnected(Long billingRunId);

    Optional<BillingRun> getBillingRunById(Long id);
}
