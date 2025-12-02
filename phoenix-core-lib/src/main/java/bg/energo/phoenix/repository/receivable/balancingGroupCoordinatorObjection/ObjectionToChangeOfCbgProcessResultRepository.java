package bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgProcessResult;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgProcessResultResponse;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ProcessMiddleResult;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionOfCbgDocumentPodImpl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ObjectionToChangeOfCbgProcessResultRepository extends JpaRepository<ObjectionToChangeOfCbgProcessResult, Long> {
    boolean existsByChangeOfCbgAndIsChecked(Long changeOfCbgId, boolean isChecked);

    Optional<ObjectionToChangeOfCbgProcessResult> findObjectionToChangeOfCbgProcessResultByIdAndChangeOfCbg(Long id, Long changeOfCbg);
    @Query(
            nativeQuery = true,
            value = """
            with cbg as
                     (select
                          distinct otcocp.pod_id,p.identifier as pod_identifier,c.customer_number,cd2.customer_id as customerId,cd2.customer_id,otcoc.id as changeOfCbgId, contract_id,cp.id as contractpodid
                      from
                          receivable.objection_to_change_of_cbg otcoc
                              join
                          receivable.objection_to_change_of_cbg_pods otcocp
                          on otcocp.change_of_cbg_id = otcoc.id
                              and otcoc.id =  :changeOfCbgId
                              join pod.pod_details pd
                                   on pd.pod_id = otcocp.pod_id
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
                      where otcocp.pod_id not in
                            (--step to skip already used pod
                                select pod_id
                                from receivable.objection_to_change_of_cbg otcoc2
                                         join receivable.objection_to_change_of_cbg_process_results otcocpr
                                              on otcocpr.change_of_cbg_id = otcoc2.id
                                                  and otcoc2.status ='ACTIVE'
                                                  and otcoc2.change_of_cbg_status = 'SEND'
                                                  and otcoc2.change_date = otcoc.change_date
                                                  and otcocpr.is_checked = true
                            )
                     )
            select
                cbg.pod_id as  podId,cbg.customer_id as customerId, cbg.changeOfCbgId as changeOfCbgId,
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
"""
    )
    List<ProcessMiddleResult> calculate(@Param("changeOfCbgId") Long changeOfCbgId);


    @Query("""
        select new bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgProcessResultResponse(ocbg.id,customer.id,customer.customerNumber,pod.id,pod.identifier,ocbg.overdueAmountForContract,ocbg.overdueAmountForBillingGroup,ocbg.overdueAmountForPod,ocbg.isChecked,bgcg.id,bgcg.name,ground.id,ground.name) from ObjectionToChangeOfCbgProcessResult ocbg
        join ObjectionToChangeOfCbg otcoc on ocbg.changeOfCbg = otcoc.id
        join Customer customer on ocbg.customer = customer.id
        join PointOfDelivery pod on pod.id = ocbg.pod
        left join BalancingGroupCoordinatorGround bgcg on bgcg.id = ocbg.balancingGroupCoordinatorGround
        left join GroundForObjectionWithdrawalToChangeOfACbg ground on ground.id = ocbg.groundForObjWithdrawalToChangeOfCbg
        where otcoc.id = :changeOfCbgId
        order by ocbg.id asc
""")
    List<ObjectionToChangeOfCbgProcessResultResponse> viewProcessResults(Long changeOfCbgId);

    @Query("""
    select ocbg.id
    from ObjectionToChangeOfCbgProcessResult ocbg
    where ocbg.changeOfCbg = :changeOfCbgId
    """)
    Set<Long> findProcessResultIdsByChangeOfCbgId(Long changeOfCbgId);

    @Query("""
    select ocbg.id
    from ObjectionToChangeOfCbgProcessResult ocbg
    join ObjectionToChangeOfCbg otcoc on ocbg.changeOfCbg = otcoc.id
    where otcoc.id != :changeOfCbgId
    and text(otcoc.changeOfCbgStatus) = 'DRAFT'
    and ocbg.pod in (
        select ocbg1.pod 
        from ObjectionToChangeOfCbgProcessResult ocbg1
        where ocbg1.changeOfCbg = :changeOfCbgId
    )
    """)
    Set<Long> findProcessResultIdsInDraftObjections(Long changeOfCbgId);

    @Query("""
        select new bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionOfCbgDocumentPodImpl(pod.identifier, podd.additionalIdentifier, customer.identifier, ocbg.overdueAmountForPod, c.name, ground.name, bgcg.name) 
        from ObjectionToChangeOfCbgProcessResult ocbg
        join ObjectionToChangeOfCbg otcoc on ocbg.changeOfCbg = otcoc.id
        join Customer customer on ocbg.customer = customer.id
        join PointOfDelivery pod on pod.id = ocbg.pod
        join PointOfDeliveryDetails podd on pod.lastPodDetailId = podd.id
        left join BalancingGroupCoordinatorGround bgcg on bgcg.id = ocbg.balancingGroupCoordinatorGround
        left join GroundForObjectionWithdrawalToChangeOfACbg ground on ground.id = ocbg.groundForObjWithdrawalToChangeOfCbg
        left join Currency c on c.defaultSelection = true
        where otcoc.id = :changeOfCbgId
        order by ocbg.id asc
""")
    List<ObjectionOfCbgDocumentPodImpl> getPodImpl(Long changeOfCbgId);

}
