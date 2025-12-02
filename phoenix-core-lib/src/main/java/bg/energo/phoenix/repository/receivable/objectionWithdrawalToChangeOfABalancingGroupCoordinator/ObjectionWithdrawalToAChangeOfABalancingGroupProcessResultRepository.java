package bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionOfCbgDocumentPodImpl;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ProcessResultFullResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface ObjectionWithdrawalToAChangeOfABalancingGroupProcessResultRepository extends JpaRepository<ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult, Long> {

    @Query("""
    select new bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ProcessResultFullResponse(
        res.id,
        customer.id,
        customer.customerNumber,
        pod.id,
        pod.identifier,
        res.overdueAmountForContract,
        res.overdueAmountForBillingGroup,
        res.overdueAmountForPod,
        res.isChecked,
        bgcg.id,
        bgcg.name,
        ground.id,
        ground.name
    )
    from ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator ocbg
    join ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult res on res.changeWithdrawalOfCbgId = ocbg.id
    join Customer customer on res.customerId = customer.id
    join PointOfDelivery pod on res.podId = pod.id
    left join BalancingGroupCoordinatorGround bgcg on bgcg.id = res.balancingGroupCoordinatorGroundId
    left join GroundForObjectionWithdrawalToChangeOfACbg ground on ground.id = res.groundForObjectionWithdrawalToChangeOfCbgId
    where ocbg.id = :withdrawalId
    and res.id in (
        select min(res2.id)
        from ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult res2
        where res2.podId = res.podId
        and res2.changeWithdrawalOfCbgId = :withdrawalId
        group by res2.podId
    )
    """)
    Page<ProcessResultFullResponse> viewProcessResults(Long withdrawalId, Pageable pageable);

    @Query("""
        select p from ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult p
        where p.id in :ids
        and p.changeWithdrawalOfCbgId=:withdrawalId
""")
    Set<ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult> findByIdsIn(List<Long> ids, Long withdrawalId);

    @Query("""
        select new bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.impl.ObjectionOfCbgDocumentPodImpl(pod.identifier, podd.additionalIdentifier, customer.identifier, ocbg.overdueAmountForPod, c.name, ground.name, bgcg.name) 
        from ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult ocbg
        join ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator otcoc on ocbg.changeWithdrawalOfCbgId = otcoc.id
        join Customer customer on ocbg.customerId = customer.id
        join PointOfDelivery pod on pod.id = ocbg.podId
        join PointOfDeliveryDetails podd on pod.lastPodDetailId = podd.id
        left join BalancingGroupCoordinatorGround bgcg on bgcg.id = ocbg.balancingGroupCoordinatorGroundId
        left join GroundForObjectionWithdrawalToChangeOfACbg ground on ground.id = ocbg.groundForObjectionWithdrawalToChangeOfCbgId
        left join Currency c on c.defaultSelection = true
        where otcoc.id = :changeOfWithdrawalId
        order by ocbg.id asc
""")
    List<ObjectionOfCbgDocumentPodImpl> getPodImpl(Long changeOfWithdrawalId);

    List<ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult> findByPodId(Long podId);
}
