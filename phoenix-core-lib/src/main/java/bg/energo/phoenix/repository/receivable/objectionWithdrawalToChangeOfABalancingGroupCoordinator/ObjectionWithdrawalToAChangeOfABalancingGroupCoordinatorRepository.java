package bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListMiddleResponse;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ProcessResultResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository extends JpaRepository<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator, Long> {

    @Query(value = "select  nextval('receivable.objection_withdrawal_to_change_of_cbg_id_seq')", nativeQuery = true)
    String getNextSequenceValue();

    boolean existsByChangeOfCbgId(Long changeOfCbgId);

    @Query(value = """
                   with cbg as
                   (select
                    distinct otcocpr.pod_id as podId,p.identifier as pod_identifier,c.customer_number,cd2.customer_id,contract_id,cp.id as contractpodid,owtcoc.id as withdrawalId,otcocpr.is_checked,owtcoc.withdrawal_change_of_cbg_status,res.is_checked as witdhrawalCheck
                   from
                    receivable.objection_withdrawal_to_change_of_cbg owtcoc                
                   join
                    receivable.objection_to_change_of_cbg_process_results otcocpr
                    on otcocpr.change_of_cbg_id = :changeOfCbgId
                    join pod.pod_details pd
                     on pd.pod_id = otcocpr.pod_id
                    join pod.pod p
                     on pd.pod_id = p.id
                    join product_contract.contract_pods cp
                     on cp.pod_detail_id  = pd.id
                     and cp.status = 'ACTIVE'
                     and current_date between cp.activation_date and coalesce(cp.deactivation_date,'2090/12/31')
                    join product_contract.contract_details cd
                      on cp.contract_detail_id = cd.id
                    join customer.customer_details cd2
                      on cd.customer_detail_id = cd2.id
                    join customer.customers c
                      on cd2.customer_id =  c.id
                    left join receivable.objection_withdrawal_to_change_of_cbg_process_results res 
                        on owtcoc.id=res.change_withdrawal_of_cbg_id  and res.pod_id=p.id
                   )
                   select
                   cbg.customer_number as customerNumber,cbg.pod_identifier as podIdentifier, cbg.podId as podId,cbg.customer_id as customerId,cbg.withdrawalId as objectionWithdrawalToCbgId,
                   (select coalesce(sum(cl.current_amount),0)  from
                     receivable.customer_liabilities cl
                      join invoice.invoices i
                     on cl.invoice_id = i.id
                      and i.product_contract_id = cbg.contract_id
                      and i.status <> 'CANCELLED'
                      and i.document_type in ('INVOICE','DEBIT_NOTE')
                      and cl.due_date < current_date
                      and cl.status = 'ACTIVE'
                      and cl.current_amount > 0
                     ) as overdueAmountForPod,
                     (select coalesce(sum(current_amount),0)
                       from receivable.customer_liabilities cl2
                       where cl2.customer_id =  cbg.customer_id
                         and cl2.due_date < current_date
                         and cl2.status = 'ACTIVE'
                         and cl2.current_amount > 0) as overdueAmountForContract,
                      (select coalesce(sum(current_amount),0)
                       from product_contract.contract_billing_groups bg
                        join receivable.customer_liabilities cl3
                         on cl3.contract_billing_group_id =  bg.id
                         and bg.contract_id = cbg.contract_id
                         and bg.status = 'ACTIVE'
                         and cl3.due_date < current_date
                         and cl3.status = 'ACTIVE'
                         and cl3.current_amount > 0
                         ) as overdueAmountForBillingGroup
                   from cbg
                   where (:alreadyExists=false and cbg.withdrawalId=:changeWithdrawalOfCbgId and cbg.is_checked=true) or (:alreadyExists=true and cbg.withdrawalId<> :changeWithdrawalOfCbgId and cbg.witdhrawalCheck=false and cbg.withdrawal_change_of_cbg_status='SEND') 
            """, nativeQuery = true)
    List<ProcessResultResponse> calculate(Long changeOfCbgId, Long changeWithdrawalOfCbgId, boolean alreadyExists);

    @Query(value = """
            select distinct
            owtcoc.withdrawal_change_of_cbg_number as withdrawalChangeOfCbgNumber,
            otcoc.change_of_cbg_number as changeOfCbgNumber,
            owtcoc.withdrawal_change_of_cbg_status as withdrawalChangeOfCbgStatus,
            owtcoc.status as status,
            count(distinct owtcocpr.pod_id) AS numberOfPods,
            date(owtcoc.create_date) AS createDate,
            owtcoc.id as id
            from
            receivable.objection_withdrawal_to_change_of_cbg owtcoc
            join
            receivable.objection_to_change_of_cbg otcoc on owtcoc.change_of_cbg_id = otcoc.id
            left join
            receivable.objection_withdrawal_to_change_of_cbg_process_results owtcocpr  on owtcocpr.change_withdrawal_of_cbg_id  = owtcoc.id
            left join
            pod.pod p on owtcocpr.pod_id = p.id
            where
            ((:withdrawalChangeOfCbgStatus) is null or text(owtcoc.withdrawal_change_of_cbg_status) in :withdrawalChangeOfCbgStatus)
            and ((:status) is null or text(owtcoc.status) in :status)
            and (DATE(:createDateFrom) is null or owtcoc.create_date >= DATE(:createDateFrom))
            and (DATE(:createDateTo) is null or owtcoc.create_date <= DATE(:createDateTo))
            and (coalesce(:prompt,'') = '' or (:searchBy = 'ALL' and coalesce(:prompt,'') <> '' and (
                                                            lower (owtcoc.withdrawal_change_of_cbg_number) like :prompt
                                                            or
                                                            lower(otcoc.change_of_cbg_number) like :prompt
                                                            or
                                                            lower(p.identifier) like :prompt
                                                            )
                                     )
                                     or (
                                         (:searchBy = 'NUMBER' and lower(owtcoc.withdrawal_change_of_cbg_number) like :prompt)
                                         or
                                         (:searchBy = 'CHANGE_OF_CBG_NUMBER' and lower(otcoc.change_of_cbg_number) like :prompt)
                                          or
                                         (:searchBy = 'POD_IDENTIFIER' and lower(P.identifier) like :prompt)
                                        ))
                                        group by
                                        owtcoc.withdrawal_change_of_cbg_number,
                                        otcoc.change_of_cbg_number,
                                        owtcoc.withdrawal_change_of_cbg_status,
                                        owtcoc.id
                                        having
                                        (coalesce(:numberOfPodsFrom,'0') = '0' or count(distinct owtcocpr.pod_id) >= :numberOfPodsFrom)
                                        and (coalesce(:numberOfPodsTo,'0') = '0' or count(distinct owtcocpr.pod_id) <= :numberOfPodsTo)
            """,
            countQuery = """
                    select
                    count(*)
                    from
                    receivable.objection_withdrawal_to_change_of_cbg owtcoc
                    join
                    receivable.objection_to_change_of_cbg otcoc on owtcoc.change_of_cbg_id = otcoc.id
                    left join
                    receivable.objection_withdrawal_to_change_of_cbg_process_results owtcocpr  on owtcocpr.change_withdrawal_of_cbg_id  = owtcoc.id
                    left join
                    pod.pod p on owtcocpr.pod_id = p.id
                    where
                    ((:withdrawalChangeOfCbgStatus) is null or text(owtcoc.withdrawal_change_of_cbg_status) in :withdrawalChangeOfCbgStatus)
                    and ((:status) is null or text(owtcoc.status) in :status)
                    and (DATE(:createDateFrom) is null or owtcoc.create_date >= DATE(:createDateFrom))
                    and (DATE(:createDateTo) is null or owtcoc.create_date <= DATE(:createDateTo))
                    and (coalesce(:prompt,'') = '' or (:searchBy = 'ALL' and coalesce(:prompt,'') <> '' and (
                                                                    lower (owtcoc.withdrawal_change_of_cbg_number) like :prompt
                                                                    or
                                                                    lower(otcoc.change_of_cbg_number) like :prompt
                                                                    or
                                                                    lower(p.identifier) like :prompt
                                                                    )
                                             )
                                             or (
                                                 (:searchBy = 'NUMBER' and lower(owtcoc.withdrawal_change_of_cbg_number) like :prompt)
                                                 or
                                                 (:searchBy = 'CHANGE_OF_CBG_NUMBER' and lower(otcoc.change_of_cbg_number) like :prompt)
                                                  or
                                                 (:searchBy = 'POD_IDENTIFIER' and lower(P.identifier) like :prompt)
                                                ))
                                                group by
                                                owtcoc.withdrawal_change_of_cbg_number,
                                                otcoc.change_of_cbg_number,
                                                owtcoc.withdrawal_change_of_cbg_status,
                                                owtcoc.id
                                                having
                                                (coalesce(:numberOfPodsFrom,'0') = '0' or count(distinct owtcocpr.pod_id) >= :numberOfPodsFrom)
                                                and (coalesce(:numberOfPodsTo,'0') = '0' or count(distinct owtcocpr.pod_id) <= :numberOfPodsTo)
                    """,
            nativeQuery = true)
    Page<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorListMiddleResponse> list(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("status") List<String> status,
            @Param("withdrawalChangeOfCbgStatus") List<String> withdrawalChangeOfCbgNumber,
            @Param("createDateFrom") LocalDate createDateFrom,
            @Param("createDateTo") LocalDate createDateTo,
            @Param("numberOfPodsFrom") Long numberOfPodsFrom,
            @Param("numberOfPodsTo") Long numberOfPodsTo,
            Pageable pageable
    );

    @Query("""
            select new bg.energo.phoenix.model.response.shared.ShortResponse(c.id,c.withdrawalChangeOfCbgNumber)
            from ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator c
            where c.changeOfCbgId = :changeOfCbgId
            and c.status = :status
            """
    )
    Optional<List<ShortResponse>> findByChangeOfCbgIdAndStatus(Long changeOfCbgId, EntityStatus status);

    @Query("""
                    select (case when count(withdrawal) > 0 then true else false end) from ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator withdrawal
                    where withdrawal.changeOfCbgId=:changeOfCbgId
                    and withdrawal.id<> :withdrawalId
                    and withdrawal.withdrawalToChangeOfCbgStatus='SEND'
            """)
    boolean existsByChangeOfCbgIdAndNotWithdrawalId(Long changeOfCbgId, Long withdrawalId);

    @Query("""
                   select ctmp
                   from ObjectionWithdrawalToCbgTemplates temp
                   join ContractTemplate ctmp on temp.templateId=ctmp.id
                   where temp.objectionToChangeWithdrawalId = :withdrawalId
                   and temp.status = 'ACTIVE'
            """)
    List<ContractTemplate> findRelatedContractTemplates(@Param("withdrawalId") Long withdrawalId);

    Optional<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator> findByIdAndStatus(
            @Param("withdrawalId") Long withdrawalId,
            @Param("status") EntityStatus status
    );

    @Query(value = """
        select result
        from ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult result
        join ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator withdrawal on withdrawal.id = result.changeWithdrawalOfCbgId
        where result.podId = :podId
        and result.isChecked
        and withdrawal.withdrawalToChangeOfCbgStatus = 'SEND'
    """)
    List<ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult> findCheckedAndSentWithdrawalPodByPodId(@Param("podId") Long podId);
}
