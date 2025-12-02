package bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupply;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;
import bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply.PodsByGridOperatorResponse;
import bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply.PodsByGridOperatorResponseDraft;
import bg.energo.phoenix.model.response.receivable.reconnectionOfPowerSupply.ReconnectionPowerSupplyListingMiddleResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconnectionOfThePowerSupplyRepository extends JpaRepository<ReconnectionOfThePowerSupply, Long> {

    @Query(value = """
                    select
                        rc.customer as customer,
                        rc.pod_identifier as podIdentifier,
                        rc.power_supply_disconnection_request_number as psdrRequestNumber,
                        rc.customer_id as customerId,
                        rc.pod_id as podId,
                        rc.request_for_disconnection_id as requestForDisconnectionId,
                        rc.grid_operator_id as gridOperatorId,
                        case when rc.liability_amount = 0 then true else false end isChecked ,
                        case when rc.liability_amount = 0 then true else false end unableToUncheck
                    from
                        (select
                             distinct
                             case when c.customer_type = 'PRIVATE_CUSTOMER'
                                      then concat(c.identifier,concat(' (',cd.name),
                                                  case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                  case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                  case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                  when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                 end customer,
                             p.identifier as pod_identifier,
                             psdr.request_number as power_supply_disconnection_request_number,
                             psdrp.customer_id,
                             psdrp.pod_id,
                             psdr.id as request_for_disconnection_id,
                             sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                             p.grid_operator_id
                    
                         --,psdrp.*
                         from
                             receivable.power_supply_disconnection_requests psdr
                                 join
                             receivable.power_supply_disconnection_request_pods psdrp
                             on psdrp.power_supply_disconnection_request_id = psdr.id
                                 and psdr.disconnection_request_status =  'EXECUTED'
                                 join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                      on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                 join
                             receivable.customer_liabilities cl
                             on psdrdl.customer_liability_id = cl.id
                                 join
                             pod.pod p
                             on psdrp.pod_id = p.id
                                 and coalesce(p.disconnected,false) = true
                                 and p.grid_operator_id = :gridOperatorId
                                 join customer.customers c
                                      on psdrp.customer_id = c.id
                                 join customer.customer_details cd
                                      on c.last_customer_detail_id = cd.id
                                 left join nomenclature.legal_forms lf
                                           on cd.legal_form_id = lf.id
                         where not exists
                             (select * from receivable.power_supply_reconnections psr2
                                                join receivable.power_supply_reconnection_pods psrp2
                                                     on psrp2.power_supply_reconnection_id = psr2.id
                                                         and psr2.reconnection_status = 'EXECUTED'
                                                         and psrp2.pod_id = psdrp.pod_id and psrp2.customer_id = psdrp.customer_id)
                           and
                             (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                 or
                                                                         text(c.customer_number) like :prompt
                                 or
                                                                         lower(p.identifier) like :prompt
                                 or
                                                                         lower(psdr.request_number) like :prompt
                                 )
                                 )
                                 or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                     or
                                     (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                     or
                                     (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                     or
                                     (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                                  )
                                 )
                        ) as rc
                    order by
                        case when rc.liability_amount = 0 then 0 else 1 end,
                        pod_id
            """, nativeQuery = true, countQuery = """
            select
                           count(rc)
                       from
                           (select
                                distinct
                                case when c.customer_type = 'PRIVATE_CUSTOMER'
                                         then concat(c.identifier,concat(' (',cd.name),
                                                     case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                     case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                     case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                     when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                    end customer,
                                p.identifier as pod_identifier,
                                psdr.request_number as power_supply_disconnection_request_number,
                                psdrp.customer_id,
                                psdrp.pod_id,
                                psdr.id as request_for_disconnection_id,
                                sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                                p.grid_operator_id
                       
                            --,psdrp.*
                            from
                                receivable.power_supply_disconnection_requests psdr
                                    join
                                receivable.power_supply_disconnection_request_pods psdrp
                                on psdrp.power_supply_disconnection_request_id = psdr.id
                                    and psdr.disconnection_request_status =  'EXECUTED'
                                    join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                         on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                    join
                                receivable.customer_liabilities cl
                                on psdrdl.customer_liability_id = cl.id
                                    join
                                pod.pod p
                                on psdrp.pod_id = p.id
                                    and coalesce(p.disconnected,false) = true
                                    and p.grid_operator_id = :gridOperatorId
                                    join customer.customers c
                                         on psdrp.customer_id = c.id
                                    join customer.customer_details cd
                                         on c.last_customer_detail_id = cd.id
                                    left join nomenclature.legal_forms lf
                                              on cd.legal_form_id = lf.id
                            where not exists
                                (select * from receivable.power_supply_reconnections psr2
                                                   join receivable.power_supply_reconnection_pods psrp2
                                                        on psrp2.power_supply_reconnection_id = psr2.id
                                                            and psr2.reconnection_status = 'EXECUTED'
                                                            and psrp2.pod_id = psdrp.pod_id and psrp2.customer_id = psdrp.customer_id)
                              and
                                (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                    or
                                                                            text(c.customer_number) like :prompt
                                    or
                                                                            lower(p.identifier) like :prompt
                                    or
                                                                            lower(psdr.request_number) like :prompt
                                    )
                                    )
                                    or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                        or
                                        (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                        or
                                        (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                        or
                                        (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                                     )
                                    )
                           ) as rc
            """)
    Page<PodsByGridOperatorResponse> findTableByGridOperatorId(@Param("gridOperatorId") Long gridOperatorId,
                                                               @Param("prompt") String prompt,
                                                               @Param("searchBy") String searchBy,
                                                               Pageable pageable);

    @Query(value = """
                    select
                                   rc.customer as customer,
                                   rc.pod_identifier as podIdentifier,
                                   rc.power_supply_disconnection_request_number as psdrRequestNumber,
                                   rc.customer_id as customerId,
                                   rc.pod_id as podId,
                                   rc.request_for_disconnection_id as requestForDisconnectionId,
                                   rc.grid_operator_id as gridOperatorId,
                               
                                   case when rc.liability_amount = 0 then true else false end isChecked ,
                                   case when rc.liability_amount = 0 then true else false end unableToUncheck
                               from
                                   (select
                                            distinct 
                                        case when c.customer_type = 'PRIVATE_CUSTOMER'
                                                 then concat(c.identifier,concat(' (',cd.name),
                                                             case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                             case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                             case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                             when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                            end customer,
                                        p.identifier as pod_identifier,
                                        psdr.request_number as power_supply_disconnection_request_number,
                                        psdrp.customer_id,
                                        psdrp.pod_id,
                                        psdr.id as request_for_disconnection_id,
                                        sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                                        p.grid_operator_id
                               
                                    --,psdrp.*
                                    from
                                        receivable.power_supply_disconnection_requests psdr
                                            join
                                        receivable.power_supply_disconnection_request_pods psdrp
                                        on psdrp.power_supply_disconnection_request_id = psdr.id
                                            and psdr.disconnection_request_status =  'EXECUTED'
                                            join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                                 on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                            join
                                        receivable.customer_liabilities cl
                                        on psdrdl.customer_liability_id = cl.id
                                            join
                                        pod.pod p
                                        on psdrp.pod_id = p.id
                                            and coalesce(p.disconnected,false) = true
                                            and p.grid_operator_id = :gridOperatorId
                                            join customer.customers c
                                                 on psdrp.customer_id = c.id
                                            join customer.customer_details cd
                                                 on c.last_customer_detail_id = cd.id
                                            left join nomenclature.legal_forms lf
                                                      on cd.legal_form_id = lf.id
                                            where not exists
                                        (select * from receivable.power_supply_reconnections psr2
                                                           join receivable.power_supply_reconnection_pods psrp2
                                                                on psrp2.power_supply_reconnection_id = psr2.id
                                                                    and psr2.reconnection_status = 'EXECUTED'
                                                                    and psrp2.pod_id = psdrp.pod_id and psrp2.customer_id = psdrp.customer_id)
                                      and
                                        (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                            or
                                                                                    text(c.customer_number) like :prompt
                                            or
                                                                                    lower(p.identifier) like :prompt
                                            or
                                                                                    lower(psdr.request_number) like :prompt
                                            )
                                            )
                                            or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                                or
                                                (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                                or
                                                (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                                or
                                                (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                                             )
                                            )
                                   ) as rc
            """, nativeQuery = true)
    List<PodsByGridOperatorResponse> findForCheck(
            @Param("gridOperatorId") Long gridOperatorId,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy
    );


    @Query(value = """
                    select
                        rc.customer as customer,
                        rc.pod_identifier as podIdentifier,
                        rc.power_supply_disconnection_request_number as psdrRequestNumber,
                        rc.cancellation_reason as cancellationReason,
                        rc.reconnection_date as reconnectionDate,
                        rc.customer_id as customerId,
                        rc.pod_id as podId,
                        rc.request_for_disconnection_id as requestForDisconnectionId,
                        rc.grid_operator_id as gridOperatorId,
                        rc.reconnection_id as reconnectionId,
                        rc.reconnection_pod_id as reconnectionPodId,
                        case when rc.reconnection_pod_id is not null then true else (case when rc.liability_amount = 0 then true else false end) end isChecked,
                        case when rc.liability_amount = 0 then true else false end unableToUncheck
                    from
                        (select
                                         distinct 
                             case when c.customer_type = 'PRIVATE_CUSTOMER'
                                      then concat(c.identifier,concat(' (',cd.name),
                                                  case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                  case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                  case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                  when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                 end customer,
                             p.identifier as pod_identifier,
                             psdr.request_number as power_supply_disconnection_request_number,
                             cr.name as cancellation_reason,
                             psrp.reconnection_date,
                             psdrp.customer_id,
                             psdrp.pod_id,
                             psdr.id as request_for_disconnection_id,
                             sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                             p.grid_operator_id,
                             psr.id as reconnection_id,
                             psrp.id as reconnection_pod_id--,
                         --psrp.is_checked
                         --,psdrp.*
                         from
                             receivable.power_supply_disconnection_requests psdr
                                 join
                             receivable.power_supply_disconnection_request_pods psdrp
                             on psdrp.power_supply_disconnection_request_id = psdr.id
                                 and psdr.disconnection_request_status =  'EXECUTED'
                                 join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                      on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                 join
                             receivable.customer_liabilities cl
                             on psdrdl.customer_liability_id = cl.id
                                 join
                             pod.pod p
                             on psdrp.pod_id = p.id
                                 and coalesce(p.disconnected,false) = true
                                 and p.grid_operator_id = :gridOperatorId
                                 join customer.customers c
                                      on psdrp.customer_id = c.id
                                 join customer.customer_details cd
                                      on c.last_customer_detail_id = cd.id
                                 left join nomenclature.legal_forms lf
                                           on cd.legal_form_id = lf.id
                                 left join
                             receivable.power_supply_reconnection_pods psrp
                             on psrp.pod_id = psdrp.pod_id
                                 and psrp.customer_id  = psdrp.customer_id
                                 and psrp.power_supply_reconnection_id=:powerSupplyReconnectionId
                                 left join
                             receivable.power_supply_reconnections psr
                             on psrp.power_supply_reconnection_id = psr.id
                                 and psr.status = 'ACTIVE'
                                 and psr.reconnection_status = 'DRAFT'
                                 left join
                             nomenclature.cancelation_reasons cr
                             on psrp.cancelation_reason_id = cr.id
                         where  not exists
                             (select * from receivable.power_supply_reconnections psr2
                                                join receivable.power_supply_reconnection_pods psrp2
                                                     on psrp2.power_supply_reconnection_id = psr2.id
                                                         and psr2.reconnection_status = 'EXECUTED'
                                                         and psrp2.pod_id = psdrp.pod_id and psrp2.customer_id = psdrp.customer_id)
                           and
                             (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                 or
                                                                         text(c.customer_number) like :prompt
                                 or
                                                                         lower(p.identifier) like :prompt
                                 or
                                                                         lower(psdr.request_number) like :prompt
                                 )
                                 )
                                 or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                     or
                                     (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                     or
                                     (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                     or
                                     (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                                  )
                                 )
                        ) as rc
            """, nativeQuery = true)
    List<PodsByGridOperatorResponseDraft> findForDraft(
            @Param("gridOperatorId") Long gridOperatorId,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("powerSupplyReconnectionId") Long powerSupplyReconnectionId
    );

    @Query(value = """
                    select
                        rc.customer as customer,
                        rc.pod_identifier as podIdentifier,
                        rc.power_supply_disconnection_request_number as psdrRequestNumber,
                        rc.cancellation_reason as cancellationReason,
                        rc.reconnection_date as reconnectionDate,
                        rc.customer_id as customerId,
                        rc.pod_id as podId,
                        rc.request_for_disconnection_id as requestForDisconnectionId,
                        rc.grid_operator_id as gridOperatorId,
                        rc.reconnection_id as reconnectionId,
                        rc.reconnection_pod_id as reconnectionPodId,
                        rc.cancellationReasonId as cancellationReasonId,
                        case when rc.reconnection_pod_id is not null then true else (case when rc.liability_amount = 0 then true else false end) end isChecked,
                        case when rc.liability_amount = 0 then true else false end unableToUncheck
                    from
                        (select
                                         distinct 
                             case when c.customer_type = 'PRIVATE_CUSTOMER'
                                      then concat(c.identifier,concat(' (',cd.name),
                                                  case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                                  case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                                  case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                                  when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                                 end customer,
                             p.identifier as pod_identifier,
                             psdr.request_number as power_supply_disconnection_request_number,
                             cr.name as cancellation_reason,
                             psrp.reconnection_date,
                             psdrp.customer_id,
                             psdrp.pod_id,
                             psdr.id as request_for_disconnection_id,
                             sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                             p.grid_operator_id,
                             psr.id as reconnection_id,
                             cr.id as cancellationReasonId,
                             psrp.id as reconnection_pod_id--,
                         --psrp.is_checked
                         --,psdrp.*
                         from
                             receivable.power_supply_disconnection_requests psdr
                                 join
                             receivable.power_supply_disconnection_request_pods psdrp
                             on psdrp.power_supply_disconnection_request_id = psdr.id
                                 and psdr.disconnection_request_status =  'EXECUTED'
                                 join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                                      on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                                 join
                             receivable.customer_liabilities cl
                             on psdrdl.customer_liability_id = cl.id
                                 join
                             pod.pod p
                             on psdrp.pod_id = p.id
                                 and coalesce(p.disconnected,false) = true
                                 and p.grid_operator_id = :gridOperatorId
                                 join customer.customers c
                                      on psdrp.customer_id = c.id
                                 join customer.customer_details cd
                                      on c.last_customer_detail_id = cd.id
                                 left join nomenclature.legal_forms lf
                                           on cd.legal_form_id = lf.id
                                 left join
                             receivable.power_supply_reconnection_pods psrp
                             on psrp.pod_id = psdrp.pod_id
                                 and psrp.customer_id  = psdrp.customer_id
                                 and psrp.power_supply_reconnection_id=:powerSupplyReconnectionId
                                 left join
                             receivable.power_supply_reconnections psr
                             on psrp.power_supply_reconnection_id = psr.id
                                 and psr.status = 'ACTIVE'
                                 and psr.reconnection_status = 'DRAFT'
                                 left join
                             nomenclature.cancelation_reasons cr
                             on psrp.cancelation_reason_id = cr.id
                         where  not exists
                             (select * from receivable.power_supply_reconnections psr2
                                                join receivable.power_supply_reconnection_pods psrp2
                                                     on psrp2.power_supply_reconnection_id = psr2.id
                                                         and psr2.reconnection_status = 'EXECUTED'
                                                         and psrp2.pod_id = psdrp.pod_id and psrp2.customer_id = psdrp.customer_id)
                           and
                             (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                 or
                                                                         text(c.customer_number) like :prompt
                                 or
                                                                         lower(p.identifier) like :prompt
                                 or
                                                                         lower(psdr.request_number) like :prompt
                                 )
                                 )
                                 or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                     or
                                     (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                     or
                                     (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                     or
                                     (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                                  )
                                 )
                        ) as rc
                        order by rc.pod_id,case when rc.reconnection_pod_id is not null then true else (case when rc.liability_amount = 0 then true else false end) end desc
            """, nativeQuery = true, countQuery = """
             select
                 count(rc)
             from
                 (select
                                  distinct 
                      case when c.customer_type = 'PRIVATE_CUSTOMER'
                               then concat(c.identifier,concat(' (',cd.name),
                                           case when cd.middle_name is not null then concat(' ',cd.middle_name)  end,
                                           case when cd.last_name is not null then concat(' ',cd.last_name)  end,
                                           case when cd.legal_form_id is not null then concat(' ',lf.name) end, ')' )
                           when c.customer_type = 'LEGAL_ENTITY' then concat(c.identifier,' (',cd.name,case when cd.legal_form_id is not null then concat(' ',lf.name) end,')')
                          end customer,
                      p.identifier as pod_identifier,
                      psdr.request_number as power_supply_disconnection_request_number,
                      cr.name as cancellation_reason,
                      psrp.reconnection_date,
                      psdrp.customer_id,
                      psdrp.pod_id,
                      psdr.id as request_for_disconnection_id,
                      sum(cl.current_amount) over (partition by psdrp.pod_id order by psdrp.pod_id) as liability_amount,
                      p.grid_operator_id,
                      psr.id as reconnection_id,
                      cr.id as cancellationReasonId,
                      psrp.id as reconnection_pod_id--,
                  --psrp.is_checked
                  --,psdrp.*
                  from
                      receivable.power_supply_disconnection_requests psdr
                          join
                      receivable.power_supply_disconnection_request_pods psdrp
                      on psdrp.power_supply_disconnection_request_id = psdr.id
                          and psdr.disconnection_request_status =  'EXECUTED'
                          join receivable.power_supply_disconnection_request_pod_liabilities psdrdl
                               on psdrdl.power_supply_disconnection_request_pod_id = psdrp.id
                          join
                      receivable.customer_liabilities cl
                      on psdrdl.customer_liability_id = cl.id
                          join
                      pod.pod p
                      on psdrp.pod_id = p.id
                          and coalesce(p.disconnected,false) = true
                          and p.grid_operator_id = :gridOperatorId
                          join customer.customers c
                               on psdrp.customer_id = c.id
                          join customer.customer_details cd
                               on c.last_customer_detail_id = cd.id
                          left join nomenclature.legal_forms lf
                                    on cd.legal_form_id = lf.id
                          left join
                      receivable.power_supply_reconnection_pods psrp
                      on psrp.pod_id = psdrp.pod_id
                          and psrp.customer_id  = psdrp.customer_id
                          and psrp.power_supply_reconnection_id=:powerSupplyReconnectionId
                          left join
                      receivable.power_supply_reconnections psr
                      on psrp.power_supply_reconnection_id = psr.id
                          and psr.status = 'ACTIVE'
                          and psr.reconnection_status = 'DRAFT'
                          left join
                      nomenclature.cancelation_reasons cr
                      on psrp.cancelation_reason_id = cr.id
                  where  not exists
                      (select * from receivable.power_supply_reconnections psr2
                                         join receivable.power_supply_reconnection_pods psrp2
                                              on psrp2.power_supply_reconnection_id = psr2.id
                                                  and psr2.reconnection_status = 'EXECUTED'
                                                  and psrp2.pod_id = psdrp.pod_id and psrp2.customer_id = psdrp.customer_id)
                    and
                      (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                          or
                                                                  text(c.customer_number) like :prompt
                          or
                                                                  lower(p.identifier) like :prompt
                          or
                                                                  lower(psdr.request_number) like :prompt
                          )
                          )
                          or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                              or
                              (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                              or
                              (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                              or
                              (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                           )
                          )
                 ) as rc
            """)
    Page<PodsByGridOperatorResponseDraft> findForDraftPage(
            @Param("gridOperatorId") Long gridOperatorId,
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("powerSupplyReconnectionId") Long powerSupplyReconnectionId,
            Pageable pageable
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
                          p.identifier as podIdentifier,
                          psdr.request_number as psdrRequestNumber,
                          cr.name as cancellationReason,
                          psrp.reconnection_date as reconnectionDate,
                          psrp.id as reconnectionPodId,
                          psrp.power_supply_reconnection_id as reconnectionId,
                          true as isChecked,
                          false as unableToUncheck,
                          c.id as customerId,
                          p.id as podId,
                          psr.grid_operator_id as gridOperatorId,
                          psrp.power_supply_disconnection_request_id as requestForDisconnectionId,
                          psrp.cancelation_reason_id as cancellationReasonId
                    from receivable.power_supply_reconnections psr
                    join
                    receivable.power_supply_reconnection_pods psrp
                    on
                    psrp.power_supply_reconnection_id = psr.id
                    and
                    psr.id = :powerSupplyReconnectionId
                    join pod.pod p
                    on psrp.pod_id = p.id
                    join customer.customers c
                    on psrp.customer_id = c.id
                    join customer.customer_details cd
                    on c.last_customer_detail_id = cd.id
                    left join nomenclature.legal_forms lf
                    on cd.legal_form_id = lf.id
                    join
                    receivable.power_supply_disconnection_requests psdr
                    on psrp.power_supply_disconnection_request_id = psdr.id
                     join
                    nomenclature.cancelation_reasons cr
                    on psrp.cancelation_reason_id = cr.id
                    and  (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                                                  or
                                                                 text(c.customer_number) like :prompt
                                                                  or
                                                                 lower(p.identifier) like :prompt
                                                                  or
                                                                 lower(psdr.request_number) like :prompt
                                                    )
                                                )
                                                or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                                     or
                                                    (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                                     or
                                                    (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                                     or
                                                    (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                                                )
                                            )
                    order by p.id
            """, nativeQuery = true, countQuery = """
                    select
                    count(psrp)
                    from receivable.power_supply_reconnections psr
                    join
                    receivable.power_supply_reconnection_pods psrp
                    on
                    psrp.power_supply_reconnection_id = psr.id
                    and
                    psr.id = :powerSupplyReconnectionId
                    join pod.pod p
                    on psrp.pod_id = p.id
                    join customer.customers c
                    on psrp.customer_id = c.id
                    join customer.customer_details cd
                    on c.last_customer_detail_id = cd.id
                    left join nomenclature.legal_forms lf
                    on cd.legal_form_id = lf.id
                    join
                    receivable.power_supply_disconnection_requests psdr
                    on psrp.power_supply_disconnection_request_id = psdr.id
                     join
                    nomenclature.cancelation_reasons cr
                    on psrp.cancelation_reason_id = cr.id
                    and  (:prompt is null or (:searchBy = 'ALL' and (lower(c.identifier) like :prompt
                                                                  or
                                                                 text(c.customer_number) like :prompt
                                                                  or
                                                                 lower(p.identifier) like :prompt
                                                                  or
                                                                 lower(psdr.request_number) like :prompt
                                                    )
                                                )
                                                or ((:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                                                     or
                                                    (:searchBy = 'CUSTOMER_NUMBER' and text(c.customer_number) like :prompt)
                                                     or
                                                    (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                                                     or
                                                    (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(psdr.request_number) like :prompt)
                                                )
                                            )
            """)
    Page<PodsByGridOperatorResponseDraft> findForExecuted(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("powerSupplyReconnectionId") Long powerSupplyReconnectionId,
            Pageable pageable
    );

    @Query("""
                    select r from ReconnectionOfThePowerSupply r
                    where r.id=:id
                    and r.status in :generalStatuses
                    and r.reconnectionStatus in :reconnectionStatuses
            """)
    Optional<ReconnectionOfThePowerSupply> findByIdAndStatusesIn(Long id, List<EntityStatus> generalStatuses, List<ReconnectionStatus> reconnectionStatuses);

    Optional<ReconnectionOfThePowerSupply> findByIdAndStatus(Long id, EntityStatus status);

    @Query(nativeQuery = true,
            value = """
                    WITH RECURSIVE pod_counts AS (
                        SELECT power_supply_reconnection_id,
                               count(distinct pod_id) as pod_count
                        FROM receivable.power_supply_reconnection_pods
                        GROUP BY power_supply_reconnection_id
                    ),
                    base_data AS (
                        SELECT DISTINCT
                            psr.id,
                            psr.reconnection_number,
                            psr.create_date,
                            psr.reconnection_status,
                            psr.status,
                            psr.grid_operator_id,
                            COALESCE(pc.pod_count, 0) as pod_count
                        FROM receivable.power_supply_reconnections psr
                        LEFT JOIN pod_counts pc ON pc.power_supply_reconnection_id = psr.id
                        WHERE ((:reconnectionEntityStatuses) is null or text(psr.status) in :reconnectionEntityStatuses)
                            AND (date(:createDateFrom) is null or date(psr.create_date) >= date(:createDateFrom))
                            AND (date(:createDateTo) is null or date(psr.create_date) <= date(:createDateTo))
                            AND (text(psr.reconnection_status) in :reconnectionStatus)
                            AND ((:gridOperatorIds) is null or psr.grid_operator_id in :gridOperatorIds)
                    ),
                    search_results AS (
                        SELECT DISTINCT
                            bd.id,
                            bd.reconnection_number as reconnectionNumber,
                            date(bd.create_date) as createDate,
                            bd.reconnection_status as reconnectionStatus,
                            go2.name as gridOperator,
                            (CASE
                                WHEN :psdrDirection = 'ASC' THEN vpsrdr.power_supply_disconnection_request_number
                                WHEN :psdrDirection = 'DESC' THEN vpsrdr.power_supply_disconnection_request_number_desc
                                ELSE vpsrdr.power_supply_disconnection_request_number
                            END) as powerSupplyDisconnectionRequestNumber,
                            bd.pod_count as numberOfPods,
                            bd.status as status
                        FROM base_data bd
                        JOIN nomenclature.grid_operators go2 ON bd.grid_operator_id = go2.id
                        LEFT JOIN receivable.vw_power_supply_reconnection_dcn_requests vpsrdr
                            ON vpsrdr.power_supply_reconnection_id = bd.id
                        LEFT JOIN receivable.power_supply_reconnection_pods psrp
                            ON psrp.power_supply_reconnection_id = bd.id
                        LEFT JOIN customer.customers c
                            ON psrp.customer_id = c.id
                        LEFT JOIN customer.customer_details cd
                            ON c.last_customer_detail_id = cd.id
                        LEFT JOIN pod.pod p
                            ON psrp.pod_id = p.id
                        WHERE (:prompt is null
                             OR (:searchBy = 'ALL' AND (
                                 lower(bd.reconnection_number) like :prompt
                                 OR lower(vpsrdr.power_supply_disconnection_request_number) like :prompt
                                 OR lower(c.identifier) like :prompt
                                 OR cd.name ILIKE :prompt
                                 OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                                 OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                                 OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                                 OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt
                                 OR lower(p.identifier) like :prompt
                             ))
                             OR (:searchBy = 'NUMBER' and lower(bd.reconnection_number) like :prompt)
                             OR (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(vpsrdr.power_supply_disconnection_request_number) like :prompt)
                             OR (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                             OR (:searchBy = 'CUSTOMER_NAME' and (
                               cd.name ILIKE :prompt
                               OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                               OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                               OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                               OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt
                               )
                             )
                             OR (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                        )
                    )
                    SELECT *
                    FROM search_results
                    WHERE (:numberOfPodsFrom is null or numberOfPods >= :numberOfPodsFrom)
                        AND (:numberOfPodsTo is null or numberOfPods <= :numberOfPodsTo)
                    """,
            countQuery = """
                    WITH RECURSIVE pod_counts AS (
                        SELECT power_supply_reconnection_id,
                               count(distinct pod_id) as pod_count
                        FROM receivable.power_supply_reconnection_pods
                        GROUP BY power_supply_reconnection_id
                    ),
                    base_data AS (
                        SELECT DISTINCT
                            psr.id,
                            psr.reconnection_number,
                            psr.create_date,
                            psr.reconnection_status,
                            psr.status,
                            psr.grid_operator_id,
                            COALESCE(pc.pod_count, 0) as pod_count
                        FROM receivable.power_supply_reconnections psr
                        LEFT JOIN pod_counts pc ON pc.power_supply_reconnection_id = psr.id
                        WHERE ((:reconnectionEntityStatuses) is null or text(psr.status) in :reconnectionEntityStatuses)
                            AND (date(:createDateFrom) is null or date(psr.create_date) >= date(:createDateFrom))
                            AND (date(:createDateTo) is null or date(psr.create_date) <= date(:createDateTo))
                            AND (text(psr.reconnection_status) in :reconnectionStatus)
                            AND ((:gridOperatorIds) is null or psr.grid_operator_id in :gridOperatorIds)
                    ),
                    search_results AS (
                        SELECT DISTINCT
                            bd.id,
                            bd.pod_count as numberOfPods
                        FROM base_data bd
                        JOIN nomenclature.grid_operators go2 ON bd.grid_operator_id = go2.id
                        LEFT JOIN receivable.vw_power_supply_reconnection_dcn_requests vpsrdr
                            ON vpsrdr.power_supply_reconnection_id = bd.id
                        LEFT JOIN receivable.power_supply_reconnection_pods psrp
                            ON psrp.power_supply_reconnection_id = bd.id
                        LEFT JOIN customer.customers c
                            ON psrp.customer_id = c.id
                        LEFT JOIN customer.customer_details cd
                            ON c.last_customer_detail_id = cd.id
                        LEFT JOIN pod.pod p
                            ON psrp.pod_id = p.id
                        WHERE (:prompt is null
                             OR (:searchBy = 'ALL' AND (
                                 lower(bd.reconnection_number) like :prompt
                                 OR lower(vpsrdr.power_supply_disconnection_request_number) like :prompt
                                 OR lower(c.identifier) like :prompt
                                 OR cd.name ILIKE :prompt
                                 OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                                 OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                                 OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                                 OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt
                                 OR lower(p.identifier) like :prompt
                             ))
                             OR (:searchBy = 'NUMBER' and lower(bd.reconnection_number) like :prompt)
                             OR (:searchBy = 'DISCONNECTION_REQUEST_NUMBER' and lower(vpsrdr.power_supply_disconnection_request_number) like :prompt)
                             OR (:searchBy = 'CUSTOMER_IDENTIFIER' and lower(c.identifier) like :prompt)
                             OR (:searchBy = 'CUSTOMER_NAME' and (
                               cd.name ILIKE :prompt
                               OR CONCAT(cd.name, ' ', cd.middle_name) ILIKE :prompt
                               OR CONCAT(cd.name, ' ', cd.last_name) ILIKE :prompt
                               OR CONCAT(cd.middle_name, ' ', cd.last_name) ILIKE :prompt
                               OR CONCAT(cd.name, ' ', middle_name, ' ', cd.last_name) ILIKE :prompt
                               )
                             )
                             OR (:searchBy = 'POD_IDENTIFIER' and lower(p.identifier) like :prompt)
                        )
                    )
                    SELECT count(1)
                    FROM search_results
                    WHERE (:numberOfPodsFrom is null or numberOfPods >= :numberOfPodsFrom)
                        AND (:numberOfPodsTo is null or numberOfPods <= :numberOfPodsTo)
                    """
    )
    Page<ReconnectionPowerSupplyListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("psdrDirection") String psdrDirection,
            @Param("createDateFrom") LocalDate createDateFrom,
            @Param("createDateTo") LocalDate createDateTo,
            @Param("reconnectionStatus") List<String> reconnectionStatus,
            @Param("numberOfPodsFrom") Long numberOfPodsFrom,
            @Param("numberOfPodsTo") Long numberOfPodsTo,
            @Param("reconnectionEntityStatuses") List<String> reconnectionEntityStatuses,
            @Param("gridOperatorIds") List<Long> gridOperatorIds,
            Pageable pageable
    );

}
