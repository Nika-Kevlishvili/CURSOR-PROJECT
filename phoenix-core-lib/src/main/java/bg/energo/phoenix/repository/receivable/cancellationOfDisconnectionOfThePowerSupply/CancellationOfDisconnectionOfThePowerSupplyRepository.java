package bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupply;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.PodsByRequestOfDcnResponseDraft;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationTableMiddleResponse;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.impl.CancellationDcnOfPwsDocumentInfoResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CancellationOfDisconnectionOfThePowerSupplyRepository extends JpaRepository<CancellationOfDisconnectionOfThePowerSupply, Long> {

    Optional<CancellationOfDisconnectionOfThePowerSupply> findByIdAndEntityStatus(Long id, EntityStatus status);

    @Query(nativeQuery = true,
        value = """
            select *
            from (select distinct psdc.cancellation_number                                 as cancellationNumber,
                                  date(psdc.create_date)                                   as cancellationDate,
                                  psdc.cancellation_status                                 as cancellationStatus,
                                  psdreq.request_number                                    as requestForDisconnectionNumber,
                                  (select count(distinct pod_id)
                                   from receivable.power_supply_dcn_cancellation_pods psdcp
                                   where psdcp.power_supply_dcn_cancellation_id = psdc.id) as numberOfPods,
                                  psdc.status                                              as entityStatus,
                                  psdc.id                                                  as id
                  from receivable.power_supply_dcn_cancellations psdc
                           join receivable.power_supply_disconnection_requests psdreq
                                on psdc.power_supply_disconnection_request_id = psdreq.id
                           join receivable.power_supply_dcn_cancellation_pods psdcp
                                on psdcp.power_supply_dcn_cancellation_id = psdc.id
                           join customer.customers c on psdcp.customer_id = c.id
                           join customer.customer_details cd on cd.customer_id = c.id
                           join pod.pod p on psdcp.pod_id = p.id
                  where (date(:createDateFrom) is null or psdc.create_date >= date(:createDateFrom))
                    and (COALESCE(:entityStatuses, NULL) IS NULL OR text(psdc.status) in (:entityStatuses))
                    and (date(:createDateTo) is null or psdc.create_date <= date(:createDateTo))
                    and (text(psdc.cancellation_status) in (:status))
                    and (coalesce(:gridOperatorId, '0') = '0' or psdreq.grid_operator_id in (:gridOperatorId))
                    and (:prompt is null or (:searchBy = 'ALL'
                      and (
                                                 lower(psdc.cancellation_number) like :prompt or
                                                 lower(psdreq.request_number) like :prompt or
                                                 c.identifier like :prompt or
                                                 lower(p.identifier) like :prompt or
                                                 cd.name ILIKE :prompt or
                                                 CONCAT(cd.name, ' ', COALESCE(cd.middle_name, '')) ILIKE :prompt or
                                                 CONCAT(cd.name, ' ', COALESCE(cd.last_name, '')) ILIKE :prompt or
                                                 CONCAT(COALESCE(cd.middle_name, ''), ' ', COALESCE(cd.last_name, '')) ILIKE
                                                 :prompt or
                                                 CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''), ' ',
                                                        COALESCE(cd.last_name, '')) ILIKE :prompt))
                      or
                         (
                             (:searchBy = 'CANCELLATION_NUMBER' and
                              lower(psdc.cancellation_number) like :prompt)
                                 or
                             (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and
                              lower(psdreq.request_number) like :prompt)
                                 or
                             (:searchBy = 'CUSTOMER_IDENTIFIER' and c.identifier like :prompt)
                                 or
                             (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                 or
                             (:searchBy = 'CUSTOMER_NAME' and (
                                 cd.name ILIKE :prompt or
                                 lower(CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''))) like :prompt or
                                 lower(CONCAT(cd.name, ' ', COALESCE(cd.last_name, ''))) like :prompt or
                                 lower(CONCAT(COALESCE(cd.middle_name, ''), ' ', COALESCE(cd.last_name, ''))) like
                                 :prompt or
                                 lower(CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''), ' ',
                                              COALESCE(cd.last_name, ''))) like :prompt)
                                 )
                             )
                      )) as tbl
            where (coalesce(:numberOfPodsFrom, '0') = '0' or tbl.numberOfPods >= :numberOfPodsFrom)
              and (coalesce(:numberOfPodsTo, '0') = '0' or tbl.numberOfPods <= :numberOfPodsTo)
            """,
        countQuery = """
            select count(*)
            from (select distinct psdc.cancellation_number                                 as cancellationNumber,
                                  date(psdc.create_date)                                   as cancellationDate,
                                  psdc.cancellation_status                                 as cancellationStatus,
                                  psdreq.request_number                                    as requestForDisconnectionNumber,
                                  (select count(distinct pod_id)
                                   from receivable.power_supply_dcn_cancellation_pods psdcp
                                   where psdcp.power_supply_dcn_cancellation_id = psdc.id) as numberOfPods,
                                  psdc.status                                              as entityStatus,
                                  psdc.id                                                  as id
                  from receivable.power_supply_dcn_cancellations psdc
                           join receivable.power_supply_disconnection_requests psdreq
                                on psdc.power_supply_disconnection_request_id = psdreq.id
                           join receivable.power_supply_dcn_cancellation_pods psdcp
                                on psdcp.power_supply_dcn_cancellation_id = psdc.id
                           join customer.customers c on psdcp.customer_id = c.id
                           join customer.customer_details cd on cd.customer_id = c.id
                           join pod.pod p on psdcp.pod_id = p.id
                  where (date(:createDateFrom) is null or psdc.create_date >= date(:createDateFrom))
                    and (COALESCE(:entityStatuses, NULL) IS NULL OR text(psdc.status) in (:entityStatuses))
                    and (date(:createDateTo) is null or psdc.create_date <= date(:createDateTo))
                    and (text(psdc.cancellation_status) in (:status))
                    and (coalesce(:gridOperatorId, '0') = '0' or psdreq.grid_operator_id in (:gridOperatorId))
                    and (:prompt is null or (:searchBy = 'ALL'
                      and (
                                                 lower(psdc.cancellation_number) like :prompt or
                                                 lower(psdreq.request_number) like :prompt or
                                                 c.identifier like :prompt or
                                                 lower(p.identifier) like :prompt or
                                                 cd.name ILIKE :prompt or
                                                 CONCAT(cd.name, ' ', COALESCE(cd.middle_name, '')) ILIKE :prompt or
                                                 CONCAT(cd.name, ' ', COALESCE(cd.last_name, '')) ILIKE :prompt or
                                                 CONCAT(COALESCE(cd.middle_name, ''), ' ', COALESCE(cd.last_name, '')) ILIKE
                                                 :prompt or
                                                 CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''), ' ',
                                                        COALESCE(cd.last_name, '')) ILIKE :prompt))
                      or
                         (
                             (:searchBy = 'CANCELLATION_NUMBER' and
                              lower(psdc.cancellation_number) like :prompt)
                                 or
                             (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and
                              lower(psdreq.request_number) like :prompt)
                                 or
                             (:searchBy = 'CUSTOMER_IDENTIFIER' and c.identifier like :prompt)
                                 or
                             (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                 or
                             (:searchBy = 'CUSTOMER_NAME' and (
                                 cd.name ILIKE :prompt or
                                 lower(CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''))) like :prompt or
                                 lower(CONCAT(cd.name, ' ', COALESCE(cd.last_name, ''))) like :prompt or
                                 lower(CONCAT(COALESCE(cd.middle_name, ''), ' ', COALESCE(cd.last_name, ''))) like
                                 :prompt or
                                 lower(CONCAT(cd.name, ' ', COALESCE(cd.middle_name, ''), ' ',
                                              COALESCE(cd.last_name, ''))) like :prompt)
                                 )
                             )
                      )) as tbl
            where (coalesce(:numberOfPodsFrom, '0') = '0' or tbl.numberOfPods >= :numberOfPodsFrom)
              and (coalesce(:numberOfPodsTo, '0') = '0' or tbl.numberOfPods <= :numberOfPodsTo)
            """)
    Page<CancellationOfDisconnectionOfThePowerSupplyListingMiddleResponse> filter(
            String prompt,
            List<Long> gridOperatorId,
            Integer numberOfPodsFrom,
            Integer numberOfPodsTo,
            LocalDateTime createDateFrom,
            LocalDateTime createDateTo,
            String searchBy,
            List<String> status,
            List<String> entityStatuses,
            Pageable pageRequest
    );


    @Query(value = """

                      select distinct rc.liability_amount,
                                         rc.customer as customer,
                                         rc.pod_identifier as podIdentifier,
                                         rc.customer_id as customerId,
                                         rc.pod_id as podId,
                                         rc.requestForDisconnectionId,
                                         case when rc.liability_amount = 0 then true else false end isChecked ,
                                         case when rc.liability_amount = 0 then true else false end unableToUncheck,
                                         case when rc.liability_amount = 0 then (select name from nomenclature.cancelation_reasons cr where cr.is_default = true) end cancellationReasonName,
                                         case when rc.liability_amount = 0 then (select id from nomenclature.cancelation_reasons cr where cr.is_default = true) end cancellationReasonId,
                                         case when rc.liability_amount = 0 then true else false end unableToChangeCancelationReason
                                  from
                                      (select psdr.id asasd,
                                              case when c.customer_type = 'PRIVATE_CUSTOMER'
                                                       then concat(c.identifier,concat(' (',cd.name),
                                                                   case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                                   case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                                   case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                                   when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                                  end customer,
                                              p.identifier as pod_identifier,
                                              psdrp.customer_id,
                                              psdrp.pod_id,
                                              psdr.id as requestForDisconnectionId,
                                              sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount
                                       from
                                           receivable.power_supply_disconnection_requests psdr
                                               join
                                           receivable.power_supply_disconnection_request_pods psdrp
                                           on psdrp.power_supply_disconnection_request_id = psdr.id
                                               and psdr.id = :powerSupplyDisconnectionRequestId
                                               and psdr.disconnection_request_status =  'EXECUTED'
                                               join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                                    on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                               join
                                           receivable.customer_liabilities cl
                                           on psdrdl.customer_liability_id = cl.id
                                               join
                                           pod.pod p
                                           on psdrp.pod_id = p.id
                                               -- and coalesce(p.disconnected,false) = false
                                               and coalesce(p.disconnected,false) = false

                                               join customer.customers c
                                                    on psdrp.customer_id = c.id
                                               join customer.customer_details cd
                                                    on c.last_customer_detail_id = cd.id
                                               left join nomenclature.legal_forms lf
                                                         on cd.legal_form_id = lf.id
                                       where not exists
                                                 (select * from receivable.power_supply_dcn_cancellations psdc
                                                                    join receivable.power_supply_dcn_cancellation_pods psdcp
                                                                         on psdcp.power_supply_dcn_cancellation_id  = psdc.id
                                                                             and psdc.cancellation_status  = 'EXECUTED'
                                                                             and psdcp.pod_id = psdrp.pod_id and psdcp.customer_id = psdrp.customer_id)
                                       and pod_id not in
                                           (Select distinct b.pod_id from receivable.power_supply_disconnections a
                                                            left join receivable.power_supply_disconnection_pods b on a.id = b.power_supply_disconnection_id
                                                            where disconnection_status = 'EXECUTED'
                                                            and status = 'ACTIVE'
                                                            and b.is_checked = true)
                                      ) as rc
            """, nativeQuery = true, countQuery = """

                        select 
                            count(rc)
                       from
                                      (select psdr.id asasd,
                                              case when c.customer_type = 'PRIVATE_CUSTOMER'
                                                       then concat(c.identifier,concat(' (',cd.name),
                                                                   case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                                   case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                                   case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                                   when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                                  end customer,
                                              p.identifier as pod_identifier,
                                              psdrp.customer_id,
                                              psdrp.pod_id,
                                              psdr.id as requestForDisconnectionId,
                                              sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount
                                       from
                                           receivable.power_supply_disconnection_requests psdr
                                               join
                                           receivable.power_supply_disconnection_request_pods psdrp
                                           on psdrp.power_supply_disconnection_request_id = psdr.id
                                               and psdr.id = :powerSupplyDisconnectionRequestId
                                               and psdr.disconnection_request_status =  'EXECUTED'
                                               join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                                    on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                               join
                                           receivable.customer_liabilities cl
                                           on psdrdl.customer_liability_id = cl.id
                                               join
                                           pod.pod p
                                           on psdrp.pod_id = p.id
                                               -- and coalesce(p.disconnected,false) = false
                                               and coalesce(p.disconnected,false) = false
                                  
                                               join customer.customers c
                                                    on psdrp.customer_id = c.id
                                               join customer.customer_details cd
                                                    on c.last_customer_detail_id = cd.id
                                               left join nomenclature.legal_forms lf
                                                         on cd.legal_form_id = lf.id
                                       where not exists
                                                 (select * from receivable.power_supply_dcn_cancellations psdc
                                                                    join receivable.power_supply_dcn_cancellation_pods psdcp
                                                                         on psdcp.power_supply_dcn_cancellation_id  = psdc.id
                                                                             and psdc.cancellation_status  = 'EXECUTED'
                                                                             and psdcp.pod_id = psdrp.pod_id and psdcp.customer_id = psdrp.customer_id)
                                       and pod_id not in
                                           (Select distinct b.pod_id from receivable.power_supply_disconnections a
                                                            left join receivable.power_supply_disconnection_pods b on a.id = b.power_supply_disconnection_id
                                                            where disconnection_status = 'EXECUTED'
                                                            and status = 'ACTIVE'
                                                            and b.is_checked = true)
                                      ) as rc
            """)
    Page<PowerSupplyDcnCancellationTableMiddleResponse> findTableByRequestOfDisconnection(@Param("powerSupplyDisconnectionRequestId") Long powerSupplyDisconnectionRequestId,
                                                                                          Pageable pageable);


    @Query(value = """
             select rc.liability_amount,
                                         rc.customer as customer,
                                         rc.pod_identifier as podIdentifier,
                                         rc.customer_id as customerId,
                                         rc.pod_id as podId,
                                         rc.requestForDisconnectionId,
                                         case when rc.liability_amount = 0 then true else false end isChecked ,
                                         case when rc.liability_amount = 0 then true else false end unableToUncheck,
                                         case when rc.liability_amount = 0 then (select name from nomenclature.cancelation_reasons cr where cr.is_default = true) end cancellationReasonName,
                                         case when rc.liability_amount = 0 then (select id from nomenclature.cancelation_reasons cr where cr.is_default = true) end cancellationReasonId,
                                         case when rc.liability_amount = 0 then true else false end unableToChangeCancelationReason
                                  from
                                      (select psdr.id asasd,
                                              case when c.customer_type = 'PRIVATE_CUSTOMER'
                                                       then concat(c.identifier,concat(' (',cd.name),
                                                                   case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                                   case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                                   case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                                   when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                                  end customer,
                                              p.identifier as pod_identifier,
                                              psdrp.customer_id,
                                              psdrp.pod_id,
                                              psdr.id as requestForDisconnectionId,
                                              sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount
                                       from
                                           receivable.power_supply_disconnection_requests psdr
                                               join
                                           receivable.power_supply_disconnection_request_pods psdrp
                                           on psdrp.power_supply_disconnection_request_id = psdr.id
                                               and psdr.id = :powerSupplyDisconnectionRequestId
                                               and psdr.disconnection_request_status =  'EXECUTED'
                                               join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                                    on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                               join
                                           receivable.customer_liabilities cl
                                           on psdrdl.customer_liability_id = cl.id
                                               join
                                           pod.pod p
                                           on psdrp.pod_id = p.id
                                               -- and coalesce(p.disconnected,false) = false
                                               and coalesce(p.disconnected,false) = false
                                  
                                               join customer.customers c
                                                    on psdrp.customer_id = c.id
                                               join customer.customer_details cd
                                                    on c.last_customer_detail_id = cd.id
                                               left join nomenclature.legal_forms lf
                                                         on cd.legal_form_id = lf.id
                                       where not exists
                                                 (select * from receivable.power_supply_dcn_cancellations psdc
                                                                    join receivable.power_supply_dcn_cancellation_pods psdcp
                                                                         on psdcp.power_supply_dcn_cancellation_id  = psdc.id
                                                                             and psdc.cancellation_status  = 'EXECUTED'
                                                                             and psdcp.pod_id = psdrp.pod_id and psdcp.customer_id = psdrp.customer_id)
                                      ) as rc
            """, nativeQuery = true)
    List<PowerSupplyDcnCancellationTableMiddleResponse> findForCheck(@Param("powerSupplyDisconnectionRequestId") Long powerSupplyDisconnectionRequestId);

    @Query(value = """
                select asasd ,rc.liability_amount,
                                  rc.customer as customer,
                                  rc.pod_identifier as podIdentifier,
                                  rc.customer_id as customerId,
                                  rc.pod_id as podId,
                                  rc.cancellationPodId,
                                  rc.podPowerSupplyDcnCancellationId,
                                  rc.requestForDisconnetionOfPowerSupplyId as requestForDisconnectionId,
                                  case when rc.liability_amount = 0 then true else false end isChecked,
                                  case when rc.liability_amount = 0 then true else false end unableToUncheck,
                                  case when rc.liability_amount = 0 then (select name from nomenclature.cancelation_reasons cr where cr.is_default = true) end cancellationReason,
                                  case when rc.liability_amount = 0 then (select id from nomenclature.cancelation_reasons cr where cr.is_default = true) end cancellationReasonId,
                                  case when rc.liability_amount = 0 then true else false end unableToChangeCancelationReason
                           
                           from
                               (select psdc2.id asasd,
                                       case when c.customer_type = 'PRIVATE_CUSTOMER'
                                                then concat(c.identifier,concat(' (',cd.name),
                                                            case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                            case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                            case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                            when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                           end customer,
                                       p.identifier as pod_identifier,
                                       psdrp.customer_id,
                                       psdrp.pod_id,
                                       sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                                       psdcp2.id as cancellationPodId,
                                       psdcp2.power_supply_dcn_cancellation_id as podPowerSupplyDcnCancellationId,
                                       psdr.id as requestForDisconnetionOfPowerSupplyId
                                       
                           
                                from
                                    receivable.power_supply_disconnection_requests psdr
                                        join
                                    receivable.power_supply_disconnection_request_pods psdrp
                                    on psdrp.power_supply_disconnection_request_id = psdr.id
                                        and psdr.id = :powerSupplyDisconnectionRequestId
                                        and psdr.disconnection_request_status =  'EXECUTED'
                                        join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                             on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                        join
                                    receivable.customer_liabilities cl
                                    on psdrdl.customer_liability_id = cl.id
                                        join
                                    pod.pod p
                                    on psdrp.pod_id = p.id
                                        and coalesce(p.disconnected,false) = false
                                        join customer.customers c
                                             on psdrp.customer_id = c.id
                                        join customer.customer_details cd
                                             on c.last_customer_detail_id = cd.id
                                        left join nomenclature.legal_forms lf
                                                  on cd.legal_form_id = lf.id
                           
                                        left join
                                    receivable.power_supply_dcn_cancellation_pods psdcp2
                                    on psdcp2.pod_id = psdrp.pod_id
                                        and psdcp2.customer_id  = psdrp.customer_id
                                        and psdcp2.power_supply_dcn_cancellation_id = :powerSupplyDcnCancellationId
                                        left join
                                    receivable.power_supply_dcn_cancellations psdc2
                                    on psdcp2.power_supply_dcn_cancellation_id  = psdc2.id
                                        and psdc2.status = 'ACTIVE'
                                        and psdc2.cancellation_status  = 'DRAFT'
                                        left join
                                    nomenclature.cancelation_reasons cr
                                    on psdcp2.cancellation_reason_id = cr.id
                           --              and  psdc2.id = :powerSupplyDcnCancellationId
                                where not exists
                                          (select * from receivable.power_supply_dcn_cancellations psdc
                                                             join receivable.power_supply_dcn_cancellation_pods psdcp
                                                                  on psdcp.power_supply_dcn_cancellation_id  = psdc.id
                                                                      and psdc.cancellation_status  = 'EXECUTED'
                                                                      and psdcp.pod_id = psdrp.pod_id and psdcp.customer_id = psdrp.customer_id)
                               ) as rc
                           
                           
            """, nativeQuery = true)
    List<PodsByRequestOfDcnResponseDraft> findForDraft(
            @Param("powerSupplyDisconnectionRequestId") Long powerSupplyDisconnectionRequestId,
            @Param("powerSupplyDcnCancellationId") Long powerSupplyDcnCancellationId
    );


    @Query(value = """

                    select
                    case when c.customer_type = 'PRIVATE_CUSTOMER'
                          then concat(c.identifier,concat(' (',cd.name),
                               case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                               case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                               case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )\s
                          when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                          end customer,
                          false as unableToUncheck,
                          p.identifier as podIdentifier,
                          cr.name as cancellationReasonName,
                          psdcp.id cancellationPodId,
                          psdcp.power_supply_dcn_cancellation_id as cancellationId,
                          psdcp.is_checked as isChecked,
                          psdcp.cancellation_reason_id as cancellationReasonId,
                          c.id as customerId,
                          p.id as podId,
                          psdc.power_supply_disconnection_request_id as requestForDisconnectionId
                    from receivable.power_supply_dcn_cancellations psdc
                    join
                    receivable.power_supply_dcn_cancellation_pods psdcp
                    on
                    psdcp.power_supply_dcn_cancellation_id = psdc.id
                    and
                    psdc.id = :powerSupplyDcnCancellationId
                    join pod.pod p
                    on psdcp.pod_id = p.id
                    join customer.customers c
                    on psdcp.customer_id = c.id
                    join customer.customer_details cd
                    on c.last_customer_detail_id = cd.id
                    left join nomenclature.legal_forms lf
                    on cd.legal_form_id = lf.id
                     join
                    nomenclature.cancelation_reasons cr
                    on psdcp.cancellation_reason_id = cr.id

            """, nativeQuery = true,
            countQuery = """
                            
                            select
                                count(psdcp)
                            from receivable.power_supply_dcn_cancellations psdc
                                     join
                                 receivable.power_supply_dcn_cancellation_pods psdcp
                                 on
                                     psdcp.power_supply_dcn_cancellation_id = psdc.id
                                         and
                                     psdc.id = :powerSupplyDcnCancellationId
                                     join pod.pod p
                                          on psdcp.pod_id = p.id
                                     join customer.customers c
                                          on psdcp.customer_id = c.id
                                     join customer.customer_details cd
                                          on c.last_customer_detail_id = cd.id
                                     left join nomenclature.legal_forms lf
                                               on cd.legal_form_id = lf.id
                                     join
                                 nomenclature.cancelation_reasons cr
                                 on psdcp.cancellation_reason_id = cr.id
                    """)
    Page<PodsByRequestOfDcnResponseDraft> findForExecuted(@Param("powerSupplyDcnCancellationId") Long powerSupplyDcnCancellationId,
                                                          Pageable pageable);

    @Query(value = """
               select asasd ,rc.liability_amount,
                                             rc.customer as customer,
                                             rc.pod_identifier as podIdentifier,
                                             rc.customer_id as customerId,
                                             rc.pod_id as podId,
                                             rc.cancellationPodId,
                                             rc.podPowerSupplyDcnCancellationId,
                                             rc.requestForDisconnetionOfPowerSupplyId as requestForDisconnectionId,
                                             case when rc.cancellationPodId is not null then true else false end isChecked,
                                             case when rc.liability_amount = 0 then true else false end unableToUncheck,
                                             rc.cancellationReasonName as cancellationReasonName,
                                             rc.cancellationReasonId as cancellationReasonId,
                                             case when rc.liability_amount = 0 then true else false end unableToChangeCancelationReason
                                      
                                      from
                                          (select distinct psdc2.id asasd,
                                                  case when c.customer_type = 'PRIVATE_CUSTOMER'
                                                           then concat(c.identifier,concat(' (',cd.name),
                                                                       case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                                       case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                                       case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                                       when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                                      end customer,
                                                  p.identifier as pod_identifier,
                                                  psdrp.customer_id,
                                                  psdrp.pod_id,
                                                  sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                                                  psdcp2.id as cancellationPodId,
                                                  psdcp2.power_supply_dcn_cancellation_id as podPowerSupplyDcnCancellationId,
                                                  psdr.id as requestForDisconnetionOfPowerSupplyId,
                                                  cr.id as cancellationReasonId,
                                                  cr.name as cancellationReasonName
                                           from
                                               receivable.power_supply_disconnection_requests psdr
                                                   join
                                               receivable.power_supply_disconnection_request_pods psdrp
                                               on psdrp.power_supply_disconnection_request_id = psdr.id
                                                   and psdr.id = :powerSupplyDisconnectionRequestId
                                                   and psdr.disconnection_request_status =  'EXECUTED'
                                                   join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                                        on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                                   join
                                               receivable.customer_liabilities cl
                                               on psdrdl.customer_liability_id = cl.id
                                                   join
                                               pod.pod p
                                               on psdrp.pod_id = p.id
                                                   and coalesce(p.disconnected,false) = false
                                                   join customer.customers c
                                                        on psdrp.customer_id = c.id
                                                   join customer.customer_details cd
                                                        on c.last_customer_detail_id = cd.id
                                                   left join nomenclature.legal_forms lf
                                                             on cd.legal_form_id = lf.id
                                      
                                                   left join
                                               receivable.power_supply_dcn_cancellation_pods psdcp2
                                               on psdcp2.pod_id = psdrp.pod_id
                                                   and psdcp2.customer_id  = psdrp.customer_id
                                                   and psdcp2.power_supply_dcn_cancellation_id = :powerSupplyDcnCancellationId
                                                   left join
                                               receivable.power_supply_dcn_cancellations psdc2
                                               on psdcp2.power_supply_dcn_cancellation_id  = psdc2.id
                                                   and psdc2.status = 'ACTIVE'
                                                   and psdc2.cancellation_status  = 'DRAFT'
                                                   left join
                                               nomenclature.cancelation_reasons cr
                                               on psdcp2.cancellation_reason_id = cr.id
                                           --              and  psdc2.id = :powerSupplyDcnCancellationId
                                           where not exists
                                                     (select * from receivable.power_supply_dcn_cancellations psdc
                                                                        join receivable.power_supply_dcn_cancellation_pods psdcp
                                                                             on psdcp.power_supply_dcn_cancellation_id  = psdc.id
                                                                                 and psdc.cancellation_status  = 'EXECUTED'
                                                                                 and psdcp.pod_id = psdrp.pod_id and psdcp.customer_id = psdrp.customer_id)
                                          ) as rc
                           
                           

            """, nativeQuery = true, countQuery = """

                 select 
                 count(rc)
                            from
                               (select distinct psdc2.id asasd,
                                                  case when c.customer_type = 'PRIVATE_CUSTOMER'
                                                           then concat(c.identifier,concat(' (',cd.name),
                                                                       case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                                       case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                                       case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                                       when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                                      end customer,
                                                  p.identifier as pod_identifier,
                                                  psdrp.customer_id,
                                                  psdrp.pod_id,
                                                  sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                                                  psdcp2.id as cancellationPodId,
                                                  psdcp2.power_supply_dcn_cancellation_id as podPowerSupplyDcnCancellationId,
                                                  psdr.id as requestForDisconnetionOfPowerSupplyId,
                                                  cr.id as cancellationReasonId,
                                                  cr.name as cancellationReasonName
                                           from
                                               receivable.power_supply_disconnection_requests psdr
                                                   join
                                               receivable.power_supply_disconnection_request_pods psdrp
                                               on psdrp.power_supply_disconnection_request_id = psdr.id
                                                   and psdr.id = :powerSupplyDisconnectionRequestId
                                                   and psdr.disconnection_request_status =  'EXECUTED'
                                                   join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                                        on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                                   join
                                               receivable.customer_liabilities cl
                                               on psdrdl.customer_liability_id = cl.id
                                                   join
                                               pod.pod p
                                               on psdrp.pod_id = p.id
                                                   and coalesce(p.disconnected,false) = false
                                                   join customer.customers c
                                                        on psdrp.customer_id = c.id
                                                   join customer.customer_details cd
                                                        on c.last_customer_detail_id = cd.id
                                                   left join nomenclature.legal_forms lf
                                                             on cd.legal_form_id = lf.id
                                      
                                                   left join
                                               receivable.power_supply_dcn_cancellation_pods psdcp2
                                               on psdcp2.pod_id = psdrp.pod_id
                                                   and psdcp2.customer_id  = psdrp.customer_id
                                                   and psdcp2.power_supply_dcn_cancellation_id = :powerSupplyDcnCancellationId
                                                   left join
                                               receivable.power_supply_dcn_cancellations psdc2
                                               on psdcp2.power_supply_dcn_cancellation_id  = psdc2.id
                                                   and psdc2.status = 'ACTIVE'
                                                   and psdc2.cancellation_status  = 'DRAFT'
                                                   left join
                                               nomenclature.cancelation_reasons cr
                                               on psdcp2.cancellation_reason_id = cr.id
                                           --              and  psdc2.id = :powerSupplyDcnCancellationId
                                           where not exists
                                                     (select * from receivable.power_supply_dcn_cancellations psdc
                                                                        join receivable.power_supply_dcn_cancellation_pods psdcp
                                                                             on psdcp.power_supply_dcn_cancellation_id  = psdc.id
                                                                                 and psdc.cancellation_status  = 'EXECUTED'
                                                                                 and psdcp.pod_id = psdrp.pod_id and psdcp.customer_id = psdrp.customer_id)
                                          ) as rc
                           
            """)
    Page<PodsByRequestOfDcnResponseDraft> findForDraftPage(@Param("powerSupplyDcnCancellationId") Long powerSupplyDcnCancellationId,
                                                           @Param("powerSupplyDisconnectionRequestId") Long powerSupplyDisconnectionRequestId,
                                                           Pageable pageable);

    @Query(value = """
                    select p.identifier                               as PODIdentifier,
                           c.customer_number                          as CustomerNumber,
                           c.identifier                               as CustomerIdentifier,
                           case
                               when c.customer_type = 'PRIVATE_CUSTOMER'
                                   then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                               else concat(cd.name, ' ', lf.name) end as CustomerNameComb,
                           cd.name                                    as CustomerName,
                           cd.middle_name                             as CustomerMiddleName,
                           cd.last_name                               as CustomerSurname,
                           pd.measurement_type                        as MeasurementType,
                           cr.name                                    as Reason,
                           psdr.power_supply_disconnection_date       as DisconnectionDate,
                           psdr.request_number                        as DisconnectionRequest,
                           concat_ws(
                                     nullif(concat_ws(' ',
                                                      case
                                                          when pd.foreign_address is true then concat(pd.district_foreign, ',')
                                                          else concat(d.name, ',') end,
                                                      case
                                                          when pd.foreign_address is true then
                                                              case
                                                                  when cd.foreign_residential_area_type is not null
                                                                      then concat(pd.foreign_residential_area_type, ' ',
                                                                                  pd.residential_area_foreign)
                                                                  else pd.residential_area_foreign
                                                                  end
                                                          else
                                                              case
                                                                  when ra.type is not null
                                                                      then concat(ra.type, ' ', ra.name)
                                                                  else ra.name
                                                                  end
                                                          end
                                            ), ''),
                                     nullif(concat_ws(' ',
                                                      case when pd.foreign_address is true then pd.foreign_street_type else s.type end,
                                                      case when pd.foreign_address is true then pd.street_foreign else s.name end,
                                                      cd.street_number
                                            ), ''),
                                     nullif(concat('бл. ', pd.block), 'бл. '),
                                     nullif(concat('вх. ', pd.entrance), 'вх. '),
                                     nullif(concat('ет. ', pd.floor), 'ет. '),
                                     nullif(concat('ап. ', pd.apartment), 'ап. '),
                                     nullif(pd.address_additional_info, '')
                           )                                          as HeadquarterAddressComb
                    from receivable.power_supply_dcn_cancellations psdc
                             join receivable.power_supply_dcn_cancellation_pods psdcp
                                 on psdc.id = psdcp.power_supply_dcn_cancellation_id
                             join receivable.power_supply_disconnection_requests psdr
                                  on psdc.power_supply_disconnection_request_id = psdr.id
                             join pod.pod p on psdcp.pod_id = p.id
                             join pod.pod_details pd on p.last_pod_detail_id = pd.id
                             join customer.customers c on psdcp.customer_id = c.id
                             join customer.customer_details cd on c.last_customer_detail_id = cd.id
                             left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                             left join nomenclature.cancelation_reasons cr on psdcp.cancellation_reason_id = cr.id
                             left join nomenclature.districts d on pd.district_id = d.id
                             left join nomenclature.residential_areas ra on pd.residential_area_id = ra.id
                             left join nomenclature.streets s on pd.street_id = s.id
                    where psdc.id = :cancellationId
            """, nativeQuery = true)
    List<CancellationDcnOfPwsDocumentInfoResponse> getDocumentInfo(@Param("cancellationId") Long cancellationId);

    @Query("""
                select ct
                from PowerSupplyDcnCancellationTemplate psdt
                join ContractTemplate ct on psdt.templateId=ct.id
                where psdt.status="ACTIVE"
                and ct.status="ACTIVE"
                and psdt.powerSupplyDcnCancellationId = :cancellationId
            """)
    List<ContractTemplate> getTemplatesIfExists(@Param("cancellationId") Long cancellationId);

}
