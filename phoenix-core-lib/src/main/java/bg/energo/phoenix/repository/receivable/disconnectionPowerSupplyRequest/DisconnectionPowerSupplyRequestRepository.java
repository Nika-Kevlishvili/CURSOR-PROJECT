package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequests;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.CustomersForDPSMiddleResponse;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.TaxCalculationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisconnectionPowerSupplyRequestRepository extends JpaRepository<DisconnectionPowerSupplyRequests, Long> {

    Optional<DisconnectionPowerSupplyRequests> findByIdAndStatus(Long id, EntityStatus entityStatus);

    @Query(value = "select nextval('receivable.power_supply_disconnection_requests_id_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    boolean existsByIdAndStatus(Long id, EntityStatus status);

    Optional<DisconnectionPowerSupplyRequests> findByIdAndDisconnectionRequestsStatusAndStatus(Long id, DisconnectionRequestsStatus disconnectionRequestsStatus, EntityStatus status);

    @Query(nativeQuery = true,
            value = """
                    select 
                        *
                    from 
                        (
                            select 
                                GRID_OPERATOR_ID as gridOperatorId,
                                psdr.request_number as requestNumber,
                                date(psdr.create_date) as createDate,
                                psdr.disconnection_request_status as disconnectionRequestStatus,
                                psdr.supplier_type as supplierType,
                                go2.name as gridOperator,
                                psdr.grid_operator_request_registration_date as gridOperatorRequestRegistrationDate,
                                psdr.customer_reminder_letter_sent_date as customerReminderLetterSentDate,
                                psdr.grid_operator_disconnection_fee_pay_date as gridOperatorDisconnectionFeePayDate,
                                psdr.power_supply_disconnection_date as powerSupplyDisconnectionDate,
                                CASE 
                                    WHEN text(psdr.disconnection_request_status) = 'DRAFT' THEN 
                                        (SELECT COUNT(DISTINCT pod_id) 
                                         FROM receivable.power_supply_disconnection_request_checked_pods psdrcp 
                                         WHERE psdrcp.power_supply_disconnection_request_id = psdr.id)
                                    WHEN text(psdr.disconnection_request_status) = 'EXECUTED' THEN 
                                        (SELECT COUNT(DISTINCT pod_id) 
                                         FROM receivable.power_supply_disconnection_request_pods psdrp 
                                         WHERE psdrp.power_supply_disconnection_request_id = psdr.id 
                                           AND psdrp.is_checked = TRUE)
                                    ELSE 0
                                END AS numberOfPods,
                                psdr.status as status,
                                psdr.id as id
                            from 
                                receivable.power_supply_disconnection_requests psdr
                            join 
                                nomenclature.grid_operators go2
                            on 
                                psdr.grid_operator_id = go2.id
                            where
                                ((:supplierType) is null or text(psdr.supplier_type) in (:supplierType))
                                and ((:gridOperatorId) is null or psdr.grid_operator_id in (:gridOperatorId))
                                and ((:status) is null or text(psdr.disconnection_request_status) in (:status))
                                and ((:entityStatuses) is null or text(psdr.status) in (:entityStatuses))
                                and (date(:gridOperatorRequestRegistrationdateFrom) is null or psdr.grid_operator_request_registration_date >= date(:gridOperatorRequestRegistrationdateFrom))
                                and (date(:gridOperatorRequestRegistrationdateTo) is null or psdr.grid_operator_request_registration_date <= date(:gridOperatorRequestRegistrationdateTo))
                                and (date(:customerReminderLetterSentDateFrom) is null or psdr.customer_reminder_letter_sent_date >= date(:customerReminderLetterSentDateFrom))
                                and (date(:customerReminderLetterSentDateTo) is null or psdr.customer_reminder_letter_sent_date <= date(:customerReminderLetterSentDateTo))
                                and (date(:gridOperatorDisconnectionFeePayDateFrom) is null or psdr.grid_operator_disconnection_fee_pay_date >= date(:gridOperatorDisconnectionFeePayDateFrom))
                                and (date(:gridOperatorDisconnectionFeePayDateTo) is null or psdr.grid_operator_disconnection_fee_pay_date <= date(:gridOperatorDisconnectionFeePayDateTo))
                                and (date(:powerSupplyDisconnectionDateFrom) is null or psdr.power_supply_disconnection_date >= date(:powerSupplyDisconnectionDateFrom))
                                and (date(:powerSupplyDisconnectionDateTo) is null or psdr.power_supply_disconnection_date <= date(:powerSupplyDisconnectionDateTo))
                        ) as tbl
                    where
                        (:numberOfPodsFrom is null or tbl.numberOfPods >= :numberOfPodsFrom)
                        and (:numberOfPodsTo is null or tbl.numberOfPods <= :numberOfPodsTo)
                        and (:prompt is null 
                             or (:searchBy = 'ALL' 
                                 and (lower(tbl.requestNumber) like lower(:prompt)
                                      or cast(tbl.numberOfPods as text) like lower(:prompt)))
                             or (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' 
                                 and lower(tbl.requestNumber) like lower(:prompt))
                             or (:searchBy = 'NUMBER_OF_PODS' 
                                 and cast(tbl.numberOfPods as text) like lower(:prompt)))
                    """,
            countQuery = """
                    select 
                        count(1)
                    from 
                        (
                            select 
                                GRID_OPERATOR_ID as gridOperatorId,
                                psdr.request_number as requestNumber,
                                date(psdr.create_date) as createDate,
                                psdr.disconnection_request_status as disconnectionRequestStatus,
                                psdr.supplier_type as supplierType,
                                go2.name as gridOperator,
                                psdr.grid_operator_request_registration_date as gridOperatorRequestRegistrationDate,
                                psdr.customer_reminder_letter_sent_date as customerReminderLetterSentDate,
                                psdr.grid_operator_disconnection_fee_pay_date as gridOperatorDisconnectionFeePayDate,
                                psdr.power_supply_disconnection_date as powerSupplyDisconnectionDate,
                                (select count(distinct pod_id) 
                                 from receivable.power_supply_disconnection_request_pods psdrp 
                                 where psdrp.power_supply_disconnection_request_id = psdr.id 
                                 and psdrp.is_checked = true) as numberOfPods,
                                psdr.status as status,
                                psdr.id as id
                            from 
                                receivable.power_supply_disconnection_requests psdr
                            join 
                                nomenclature.grid_operators go2
                            on 
                                psdr.grid_operator_id = go2.id
                            where
                                ((:supplierType) is null or text(psdr.supplier_type) in (:supplierType))
                                and ((:gridOperatorId) is null or psdr.grid_operator_id in (:gridOperatorId))
                                and ((:status) is null or text(psdr.disconnection_request_status) in (:status))
                                and ((:entityStatuses) is null or text(psdr.status) in (:entityStatuses))
                                and (date(:gridOperatorRequestRegistrationdateFrom) is null or psdr.grid_operator_request_registration_date >= date(:gridOperatorRequestRegistrationdateFrom))
                                and (date(:gridOperatorRequestRegistrationdateTo) is null or psdr.grid_operator_request_registration_date <= date(:gridOperatorRequestRegistrationdateTo))
                                and (date(:customerReminderLetterSentDateFrom) is null or psdr.customer_reminder_letter_sent_date >= date(:customerReminderLetterSentDateFrom))
                                and (date(:customerReminderLetterSentDateTo) is null or psdr.customer_reminder_letter_sent_date <= date(:customerReminderLetterSentDateTo))
                                and (date(:gridOperatorDisconnectionFeePayDateFrom) is null or psdr.grid_operator_disconnection_fee_pay_date >= date(:gridOperatorDisconnectionFeePayDateFrom))
                                and (date(:gridOperatorDisconnectionFeePayDateTo) is null or psdr.grid_operator_disconnection_fee_pay_date <= date(:gridOperatorDisconnectionFeePayDateTo))
                                and (date(:powerSupplyDisconnectionDateFrom) is null or psdr.power_supply_disconnection_date >= date(:powerSupplyDisconnectionDateFrom))
                                and (date(:powerSupplyDisconnectionDateTo) is null or psdr.power_supply_disconnection_date <= date(:powerSupplyDisconnectionDateTo))
                        ) as tbl
                    where
                        (:numberOfPodsFrom is null or tbl.numberOfPods >= :numberOfPodsFrom)
                        and (:numberOfPodsTo is null or tbl.numberOfPods <= :numberOfPodsTo)
                        and (:prompt is null 
                             or (:searchBy = 'ALL' 
                                 and (lower(tbl.requestNumber) like lower(:prompt)
                                      or cast(tbl.numberOfPods as text) like lower(:prompt)))
                             or (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' 
                                 and lower(tbl.requestNumber) like lower(:prompt))
                             or (:searchBy = 'NUMBER_OF_PODS' 
                                 and cast(tbl.numberOfPods as text) like lower(:prompt)))
                    """)
    Page<DisconnectionRequestListingMiddleResponse> filter(
            @Param("supplierType") List<String> supplierType,
            @Param("gridOperatorId") List<Long> gridOperatorId,
            @Param("status") List<String> status,
            @Param("gridOperatorRequestRegistrationdateFrom") LocalDate gridOperatorRequestRegistrationDateFrom,
            @Param("gridOperatorRequestRegistrationdateTo") LocalDate gridOperatorRequestRegistrationDateTo,
            @Param("customerReminderLetterSentDateFrom") LocalDate customerReminderLetterSentDateFrom,
            @Param("customerReminderLetterSentDateTo") LocalDate customerReminderLetterSentDateTo,
            @Param("gridOperatorDisconnectionFeePayDateFrom") LocalDate gridOperatorDisconnectionFeePayDateFrom,
            @Param("gridOperatorDisconnectionFeePayDateTo") LocalDate gridOperatorDisconnectionFeePayDateTo,
            @Param("powerSupplyDisconnectionDateFrom") LocalDate powerSupplyDisconnectionDateFrom,
            @Param("powerSupplyDisconnectionDateTo") LocalDate powerSupplyDisconnectionDateTo,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("numberOfPodsFrom") Long numberOfPodsFrom,
            @Param("numberOfPodsTo") Long numberOfPodsTo,
            @Param("entityStatuses") List<String> entityStatuses,
            Pageable pageable
    );

    @Query(nativeQuery = true,
            value = """
                    WITH highest_consumption_pod AS (
                    SELECT
                        cp.contract_billing_group_id,
                        pd.id AS pod_detail_id,
                        pd.estimated_monthly_avg_consumption,
                        ROW_NUMBER() OVER (
                            PARTITION BY cp.contract_billing_group_id
                            ORDER BY pd.estimated_monthly_avg_consumption DESC, pd.create_date DESC
                        ) AS row_num
                    FROM
                        product_contract.contract_pods cp
                    JOIN
                        pod.pod_details pd ON cp.pod_detail_id = pd.id
                )
                select
                    tbl.customer as customer,
                    array_to_string(array(select distinct unnest(string_to_array(tbl.contracts, ','))),',') as contracts,
                    array_to_string(array(select distinct unnest(string_to_array(tbl.alt_recipient_inv_customer, ','))),',') as altRecipientInvCustomer,
                    array_to_string(array(select distinct unnest(string_to_array(tbl.billing_groups, ','))),',') as billingGroups,
                    tbl.pod_identifier as podIdentifier,
                    tbl.is_highest_consumption isHighestConsumption,
                    array_to_string(array(select distinct unnest(string_to_array(tbl.liabilities_in_billing_group, ','))),',') as liabilitiesInBillingGroup,
                    array_to_string(array(select distinct unnest(string_to_array(tbl.liabilities_in_pod, ','))),',') as liabilitiesInPod,
                    tbl.podid as podId,
                    tbl.podDetailId as podDetailId,
                    tbl.customer_id as customerId,
                    tbl.grid_operator_id as gridOperatorId,
                    tbl.liability_amount_customer as liabilityAmountCustomer,
                    tbl.existing_customer_receivables as existingCustomerReceivables,
                    tbl.invoice_number as invoiceNumber,
                    tbl.customer_number as customerNumber,
                    tbl.powerSupplyDisconnectionReminderId
                from
                    (select distinct
                         c.identifier,
                         cl.id as customer_liability_id,
                         case when c.customer_type = 'PRIVATE_CUSTOMER'
                                  then concat(c.identifier,concat(' (',cd.name),
                                              case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                              case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                              case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                              when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                             end customer,
                         p.grid_operator_id,
                         psdrc.customer_id,
                         p.id as podid,
                         pd.id as podDetailId,
                         i.invoice_number as invoice_number,
                         p.identifier as pod_identifier,
                         c.customer_number as customer_number,
                         sum(cl.current_amount) over (partition by p.id order by 1) AS liability_amount_customer,
                         string_agg(cbg.group_number,',') over (partition by p.id order by cbg.group_number rows between unbounded preceding and unbounded following) as billing_groups,
                         case when cbg.separate_invoice_for_each_pod = false
                                  then
                                          string_agg(concat(cl.current_amount,'-',ccy.name,'-',cl.liability_number),',')
                                          over (partition by p.id order by cl.liability_number rows between unbounded preceding and unbounded following)
                             end liabilities_in_billing_group,
                         string_agg(case when c1.customer_type = 'PRIVATE_CUSTOMER'
                                             then concat(c1.identifier,concat(' (',cd.name),
                                                         case when cd1.middle_name is not null then concat(' ',cd1.middle_name)  end,
                                                         case when cd1.last_name is not null then concat(' ',cd1.last_name)  end,
                                                         case when cd1.legal_form_id is not null then concat(' ',lf1.name) end, ')')
                                         when c1.customer_type = 'LEGAL_ENTITY' then concat(c1.identifier,' (',cd1.name,case when cd1.legal_form_id is not null then concat(' ',lf1.name) end,')')
                             end,',') over (partition by cbg.id order by 1  rows between unbounded preceding and unbounded following) as alt_recipient_inv_customer,
                         string_agg(contr.contract_number,',') over (partition by cl.customer_id order by 1  rows between unbounded preceding and unbounded following) as contracts,
                         case when cbg.separate_invoice_for_each_pod = true
                                  then string_agg(concat(cl.current_amount,'-',ccy.name,'-',cl.liability_number), ',')
                                       OVER (PARTITION BY p.id ORDER BY cl.liability_number
                                           rows between unbounded preceding and unbounded following)
                             end liabilities_in_pod,
                         row_number() over(partition by p.id order by cl.create_date desc) as last_liability_customer,
                         case when cusrec.id is not null then 'YES' else 'NO' end as existing_customer_receivables,
                         psdr.id as powerSupplyDisconnectionReminderId,
                         CASE WHEN hc.row_num = 1 THEN true
                              ELSE false
                         END AS is_highest_consumption,
                         receivable.check_customer_in_mofb(p_customer_id => c.id,
                                                           p_blocked_for_payment => false,
                                                           p_blocked_for_reminder_letters => false,
                                                           p_blocked_for_calculation_of_late_payment => false,
                                                           p_blocked_for_liabilities_offsetting => false,
                                                           p_blocked_for_supply_termination => true
                         ) as customer_is_blocked
                     from
                         receivable.power_supply_disconnection_reminders psdr
                             join
                         receivable.power_supply_disconnection_reminder_customers psdrc
                         on psdrc.power_supply_disconnection_reminder_id = psdr.id
                             and psdr.id = :powerSupplyDisconnectionReminderId
                             join
                         receivable.customer_liabilities cl
                         on cl.id = psdrc.customer_liability_id
                             and cl.status = 'ACTIVE'
                             and cl.due_date <= current_date
                             and cl.due_date <= psdr.liabilities_max_due_date
                             and cl.current_amount > 0
                             and (case when coalesce(cl.blocked_for_supply_termination,false) = true
                                           then
                                                   current_date between cl.blocked_for_supply_termination_from_date  and
                                               coalesce(cl.blocked_for_supply_termination_to_date,current_date)
                                       else 1=1 end)
                             join nomenclature.currencies ccy on cl.currency_id = ccy.id
                             join product_contract.contract_billing_groups cbg
                                  on cl.contract_billing_group_id = cbg.id
                             join
                         invoice.invoices i
                         on cl.invoice_id = i.id
                             and i.contract_billing_group_id is not null
                             join
                         invoice.invoice_standard_detailed_data isdd
                         on isdd.invoice_id = i.id
                             left join receivable.customer_receivables cusrec on
                             cusrec.customer_id = cl.customer_id
                                             and cusrec.status = 'ACTIVE'
                                             and cusrec.current_amount > 0
                             join
                         pod.pod p
                         on isdd.pod_id = p.id
                             and ((:excludePodIds) is null or p.id not in (:excludePodIds))
                             and  coalesce(p.disconnected,false) = false
                             and p.impossibility_disconnection = false
                             and p.grid_operator_id = :gridOperatorId
                             and p.status = 'ACTIVE'
                             and
                            case when p.blocked_for_disconnection = true
                                     then
                                             current_date + INTERVAL '1 day' between p.blocked_for_disconnection_date_from  and
                                         coalesce(p.blocked_for_disconnection_date_to,current_date)
                                 else 1=1 end
                             join
                         pod.pod_details pd
                         on pd.pod_id = p.id
                                LEFT JOIN highest_consumption_pod hc
                        ON cbg.id = hc.contract_billing_group_id
                        AND pd.id = hc.pod_detail_id
                             join
                         customer.customers c
                         on cl.customer_id = c.id
                             left join receivable.customer_liabilities l on l.id = psdrc.customer_liability_id
                             join
                         customer.customer_details cd
                         on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf
                                       on cd.legal_form_id = lf.id
                             left join customer.customer_details cd1
                                       on cd1.id = cbg.alt_invoice_recipient_customer_detail_id
                             left join
                         customer.customers c1
                         on c1.last_customer_detail_id = cd1.id
                             left join nomenclature.legal_forms lf1
                                       on cd1.legal_form_id = lf1.id
                             join product_contract.contracts contr
                                  on i.product_contract_id = contr.id
                     where not exists
                         (select 1 from receivable.power_supply_disconnection_requests psdr1
                                            join receivable.power_supply_disconnection_request_pods psdrp1
                                                 on psdrp1.power_supply_disconnection_request_id = psdr1.id
                                                     and psdr1.disconnection_request_status = 'EXECUTED'
                                                     and psdrp1.pod_id = p.id)
                       and
                         (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                             or
                                                                     text(c.customer_number) like :prompt
                             or
                                                                     lower(contr.contract_number) like :prompt
                             or
                                                                     lower(cbg.group_number) like :prompt
                             or
                                                                     lower(p.identifier) like :prompt
                             or
                                                                     lower(cl.liability_number) like :prompt
                             or
                                                                     lower(i.invoice_number) like :prompt
                             )
                             )
                             or (
                              (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                  or
                              (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                  or
                              (:searchBy = 'CONTRACT_NUMBER' and lower(contr.contract_number) like :prompt)
                                  or
                              (:searchBy = 'BILLING_GROUP_NUMBER' and lower(cbg.group_number) like :prompt)
                                  or
                              (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                  or
                              (:searchBy = 'LIABILITY_NUMBER' and lower(cl.liability_number) like :prompt)
                                  or
                              (:searchBy = 'OUTGOING_DOCUMENT_NUMBER' and lower(i.invoice_number) like :prompt)
                              )
                             )
                    ) as tbl
                where tbl.last_liability_customer = 1
                  and tbl.customer_is_blocked = 0
                  and
                    (:liabilityAmountFrom is null or tbl.liability_amount_customer >=  :liabilityAmountFrom)
                  and
                    (:liabilityAmountTo is null or  tbl.liability_amount_customer <=  :liabilityAmountTo)
                  and
                    (:isHighestConsumption is null or  text(tbl.is_highest_consumption) like text(concat('%',:isHighestConsumption,'%')))
                  and
                    case when :customerConditionType = 'LIST_OF_CUSTOMERS'
                             then trim(tbl.identifier) = ANY(ARRAY(SELECT trim(both ' ' from unnest(string_to_array(:listOfCustomers, ',')))))
                         when :customerConditionType = 'ALL_CUSTOMERS'
                             then
                             1=1
                         when :customerConditionType = 'CUSTOMERS_UNDER_CONDITIONS'
                             then
                             tbl.customer_liability_id  in (select receivable.liability_condition_eval(:customerConditions))
                        end
                order by :sortByField
                                                           """,
            countQuery = """
            WITH highest_consumption_pod AS (
            SELECT
                cp.contract_billing_group_id,
                pd.id AS pod_detail_id,
                pd.estimated_monthly_avg_consumption,
                ROW_NUMBER() OVER (
                    PARTITION BY cp.contract_billing_group_id
                    ORDER BY pd.estimated_monthly_avg_consumption DESC, pd.create_date DESC
                ) AS row_num
            FROM
                product_contract.contract_pods cp
            JOIN
                pod.pod_details pd ON cp.pod_detail_id = pd.id
        )
        
        
        select
            count(*)
        from
            (select distinct
                 c.identifier,
                 cl.id as customer_liability_id,
                 case when c.customer_type = 'PRIVATE_CUSTOMER'
                          then concat(c.identifier,concat(' (',cd.name),
                                      case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                      case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                      case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                      when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                     end customer,
                 p.grid_operator_id,
                 psdrc.customer_id,
                 p.id as podid,
                 pd.id as podDetailId,
                 i.invoice_number as invoice_number,
                 p.identifier as pod_identifier,
                 c.customer_number as customer_number,
                 sum(cl.current_amount) over (partition by p.id order by 1) AS liability_amount_customer,
                 string_agg(cbg.group_number,',') over (partition by p.id order by cbg.group_number rows between unbounded preceding and unbounded following) as billing_groups,
                 case when cbg.separate_invoice_for_each_pod = false
                          then
                                  string_agg(concat(cl.current_amount,'-',ccy.name,'-',cl.liability_number),',')
                                  over (partition by p.id order by cl.liability_number rows between unbounded preceding and unbounded following)
                     end liabilities_in_billing_group,
                 string_agg(case when c1.customer_type = 'PRIVATE_CUSTOMER'
                                     then concat(c1.identifier,concat(' (',cd.name),
                                                 case when cd1.middle_name is not null then concat(' ',cd1.middle_name)  end,
                                                 case when cd1.last_name is not null then concat(' ',cd1.last_name)  end,
                                                 case when cd1.legal_form_id is not null then concat(' ',lf1.name) end, ')')
                                 when c1.customer_type = 'LEGAL_ENTITY' then concat(c1.identifier,' (',cd1.name,case when cd1.legal_form_id is not null then concat(' ',lf1.name) end,')')
                     end,',') over (partition by cbg.id order by 1  rows between unbounded preceding and unbounded following) as alt_recipient_inv_customer,
                 string_agg(contr.contract_number,',') over (partition by cl.customer_id order by 1  rows between unbounded preceding and unbounded following) as contracts,
                 case when cbg.separate_invoice_for_each_pod = true
                          then string_agg(concat(cl.current_amount,'-',ccy.name,'-',cl.liability_number), ',')
                               OVER (PARTITION BY p.id ORDER BY cl.liability_number
                                   rows between unbounded preceding and unbounded following)
                     end liabilities_in_pod,
                 row_number() over(partition by p.id order by cl.create_date desc) as last_liability_customer,
                case when cusrec.id is not null then 'YES' else 'NO' end as existing_customer_receivables,
                 psdr.id as powerSupplyDisconnectionReminderId,
                 CASE WHEN hc.row_num = 1 THEN true
                      ELSE false
                 END AS is_highest_consumption,
                 receivable.check_customer_in_mofb(p_customer_id => c.id,
                                                   p_blocked_for_payment => false,
                                                   p_blocked_for_reminder_letters => false,
                                                   p_blocked_for_calculation_of_late_payment => false,
                                                   p_blocked_for_liabilities_offsetting => false,
                                                   p_blocked_for_supply_termination => true
                 ) as customer_is_blocked
             from
                 receivable.power_supply_disconnection_reminders psdr
                     join
                 receivable.power_supply_disconnection_reminder_customers psdrc
                 on psdrc.power_supply_disconnection_reminder_id = psdr.id
                     and psdr.id = :powerSupplyDisconnectionReminderId
                     join
                 receivable.customer_liabilities cl
                 on cl.id = psdrc.customer_liability_id
                     and cl.status = 'ACTIVE'
                     and cl.due_date <= current_date
                     and cl.due_date <= psdr.liabilities_max_due_date
                     and cl.current_amount > 0
                     and (case when coalesce(cl.blocked_for_supply_termination,false) = true
                                   then
                                           current_date between cl.blocked_for_supply_termination_from_date  and
                                       coalesce(cl.blocked_for_supply_termination_to_date,current_date)
                               else 1=1 end)
                     join nomenclature.currencies ccy on cl.currency_id = ccy.id
                     join product_contract.contract_billing_groups cbg
                          on cl.contract_billing_group_id = cbg.id
                     join
                 invoice.invoices i
                 on cl.invoice_id = i.id
                     and i.contract_billing_group_id is not null
                     join
                 invoice.invoice_standard_detailed_data isdd
                 on isdd.invoice_id = i.id
                     left join receivable.customer_receivables cusrec on
                                                  cusrec.customer_id = cl.customer_id
                                             and cusrec.status = 'ACTIVE'
                                             and cusrec.current_amount > 0
                     join
                 pod.pod p
                 on isdd.pod_id = p.id
                     and ((:excludePodIds) is null or p.id not in (:excludePodIds))
                     and  coalesce(p.disconnected,false) = false
                     and p.impossibility_disconnection = false
                     and p.grid_operator_id = :gridOperatorId
                     and p.status = 'ACTIVE'
                     and
                    case when p.blocked_for_disconnection = true
                             then
                                     current_date + INTERVAL '1 day' between p.blocked_for_disconnection_date_from  and
                                 coalesce(p.blocked_for_disconnection_date_to,current_date)
                         else 1=1 end
                     join
                 pod.pod_details pd
                 on pd.pod_id = p.id
                        LEFT JOIN highest_consumption_pod hc
                ON cbg.id = hc.contract_billing_group_id
                AND pd.id = hc.pod_detail_id
                     join
                 customer.customers c
                 on cl.customer_id = c.id
                     left join receivable.customer_liabilities l on l.id = psdrc.customer_liability_id
                     join
                 customer.customer_details cd
                 on c.last_customer_detail_id = cd.id
                     left join nomenclature.legal_forms lf
                               on cd.legal_form_id = lf.id
                     left join customer.customer_details cd1
                               on cd1.id = cbg.alt_invoice_recipient_customer_detail_id
                     left join
                 customer.customers c1
                 on c1.last_customer_detail_id = cd1.id
                     left join nomenclature.legal_forms lf1
                               on cd1.legal_form_id = lf1.id
                     join product_contract.contracts contr
                          on i.product_contract_id = contr.id
             where not exists
                 (select 1 from receivable.power_supply_disconnection_requests psdr1
                                    join receivable.power_supply_disconnection_request_pods psdrp1
                                         on psdrp1.power_supply_disconnection_request_id = psdr1.id
                                             and psdr1.disconnection_request_status = 'EXECUTED'
                                             and psdrp1.pod_id = p.id)
               and
                 (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                     or
                                                             text(c.customer_number) like :prompt
                     or
                                                             lower(contr.contract_number) like :prompt
                     or
                                                             lower(cbg.group_number) like :prompt
                     or
                                                             lower(p.identifier) like :prompt
                     or
                                                             lower(cl.liability_number) like :prompt
                     or
                                                             lower(i.invoice_number) like :prompt
                     )
                     )
                     or (
                      (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                          or
                      (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                          or
                      (:searchBy = 'CONTRACT_NUMBER' and lower(contr.contract_number) like :prompt)
                          or
                      (:searchBy = 'BILLING_GROUP_NUMBER' and lower(cbg.group_number) like :prompt)
                          or
                      (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                          or
                      (:searchBy = 'LIABILITY_NUMBER' and lower(cl.liability_number) like :prompt)
                          or
                      (:searchBy = 'OUTGOING_DOCUMENT_NUMBER' and lower(i.invoice_number) like :prompt)
                      )
                     )
            ) as tbl
        where tbl.last_liability_customer = 1
          and tbl.customer_is_blocked = 0
          and
            (:liabilityAmountFrom is null or tbl.liability_amount_customer >=  :liabilityAmountFrom)
          and
            (:liabilityAmountTo is null or  tbl.liability_amount_customer <=  :liabilityAmountTo)
          and
            (:isHighestConsumption is null or  text(tbl.is_highest_consumption) like text(concat('%',:isHighestConsumption,'%')))
          and
            case when :customerConditionType = 'LIST_OF_CUSTOMERS'
                     then trim(tbl.identifier) = ANY(ARRAY(SELECT trim(both ' ' from unnest(string_to_array(:listOfCustomers, ',')))))
                 when :customerConditionType = 'ALL_CUSTOMERS'
                     then
                     1=1
                 when :customerConditionType = 'CUSTOMERS_UNDER_CONDITIONS'
                     then
                     tbl.customer_liability_id  in (select receivable.liability_condition_eval(:customerConditions))
                end
"""
    )
    Page<CustomersForDPSMiddleResponse> customersForDPS(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("powerSupplyDisconnectionReminderId") Long powerSupplyDisconnectionReminderId,
            @Param("excludePodIds") List<Long> excludePodIds,
            @Param("gridOperatorId") Long gridOperatorId,
            @Param("liabilityAmountFrom") BigDecimal liabilityAmountFrom,
            @Param("liabilityAmountTo") BigDecimal liabilityAmountTo,
            @Param("isHighestConsumption") Boolean isHighestConsumption,
            @Param("sortByField") String sortByField,
            @Param("customerConditions") String customerConditions,
            @Param("customerConditionType") String customerConditionType,
            @Param("listOfCustomers") String listOfCustomers,
            Pageable pageable
    );

    @Query(value = """
    select mofb.condition_text from receivable.liabilities_condition_replacemets mofb
    where mofb.is_key = true
""", nativeQuery = true)
    List<String> getConditionKeys();

    @Query(value = """
    Select receivable.overdue_liability_condition_eval(:condition) as id
    """, nativeQuery = true)
    List<Long> getOverdueLiabilitiesByCondition (@Param("condition") String  condition);

    @Query(value = """
                    select cl.id as liabilityId, pod.id,
                                                    cl.customer_id as customerId, cl.contract_billing_group_id as billingGroupId, cl.invoice_id as invoiceId,
                                                    tax.email_template_id emailTemplateId, tax.document_template_id documentTemplateId,
                                                    tax.number_of_income_account numberOfIncomeAccount, tax.basis_for_issuing basisForIssuing,
                                                    tax.cost_center_controlling_order costCenterControllingOrder, tax.tax_for_reconnection taxForReconnection,
                                                    tax.price_component_or_price_component_group_or_item priceComponent,
                                                    tax.currency_id currencyId,
                                                    dpsr.id as resultId
                                             from receivable.power_supply_disconnection_request_results dpsr
                                                      join receivable.power_supply_disconnection_requests dps on dpsr.power_supply_disconnection_request_id = dps.id
                                                      join pod.pod_details podd on dpsr.pod_detail_id = podd.id
                                                      join pod.pod pod on podd.pod_id = pod.id
                                                      join nomenclature.grid_operator_taxes tax on (tax.supplier_type::text) = dps.supplier_type::text
                                                 and tax.grid_operator_id = dps.grid_operator_id
                                                 and (
                                                                                                       (podd.measurement_type = 'SETTLEMENT_PERIOD' and tax.default_for_pod_with_measurement_type_by_settlement_period = true)
                                                                                                           or
                                                                                                       (podd.measurement_type = 'SLP' and tax.default_for_pod_with_measurement_type_slp = true)
                                                                                                       )
                                                      join receivable.customer_liabilities cl on cl.liability_number = (
                                                 select cl1.liability_number
                                                 from receivable.customer_liabilities cl1
                                                          join (
                                                     select substring(liability_item from '[^-]+-[^-]+-(.+)') as extracted_liability
                                                     from (
                                                              select unnest(string_to_array(COALESCE(dpsr.liabilities_in_pod, dpsr.liabilities_in_billing_group), ',')) as liability_item
                                                          ) as li
                                                 ) extracted_liabilities on cl1.liability_number = extracted_liabilities.extracted_liability
                                                 order by cl1.create_date desc
                                                 limit 1
                                             )
                                             where dpsr.is_checked = true
                                               and tax.status = 'ACTIVE'
                                               and dps.id = :requestId
            """,nativeQuery = true)
    List<TaxCalculationResponse> fetchLiabilities(@Param("requestId") Long requestId);

}
