package bg.energo.phoenix.repository.contract.action;

import bg.energo.phoenix.model.documentModels.action.ActionPodModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.ActionPod;
import bg.energo.phoenix.model.response.contract.action.ActionPodResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActionPodRepository extends JpaRepository<ActionPod, Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.action.ActionPodResponse(
                            p.id,
                            p.identifier
                    )
                    from ActionPod ap
                    join PointOfDelivery p on p.id = ap.podId
                        where ap.actionId = :actionId
                        and ap.status in :statuses
                    """
    )
    List<ActionPodResponse> fetchShortResponseByActionIdAndStatusIn(
            @Param("actionId") Long actionId,
            @Param("statuses") List<EntityStatus> statuses
    );


    List<ActionPod> findByActionIdAndStatusIn(Long actionId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select ap.podId from ActionPod ap
                        where ap.actionId = :actionId
                        and ap.status in :statuses
                    """
    )
    List<Long> findPodIdsByActionIdAndStatusIn(Long actionId, List<EntityStatus> statuses);


    @Query(
            nativeQuery = true,
            value = """
                    select count(1) from(
                        select unnest(cast(string_to_array((:currActionPodIds), ',') as bigint[]))
                        except
                            select ap.pod_id from action.action_pods ap
                            where ap.action_id in :persistedActionIds and ap.status = 'ACTIVE'
                    ) as tbl
                    """
    )
    Long countPodsNotCoveredByPersistedActions(
            @Param("currActionPodIds") String currActionPodIds,
            @Param("persistedActionIds") List<Long> persistedActionIds
    );


    @Query(
            nativeQuery = true,
            value = """
                    select count(1) from (
                        select distinct pd.pod_id
                            from product_contract.contract_details cd
                            join product_contract.contract_pods cp on cp.contract_detail_id = cd.id and cp.status = 'ACTIVE'
                            join pod.pod_details pd on pd.id =  cp.pod_detail_id
                                where cd.contract_id = :contractId
                                and cd.start_date >= coalesce((
                                    select max(start_date)
                                    from product_contract.contract_details cd1
                                        where cd1.contract_id = cd.contract_id
                                        and start_date <= :executionDate), cd.start_date
                                )
                        except
                            select distinct pd.pod_id
                            from product_contract.contract_details cd
                            join product_contract.contract_pods cp on cp.contract_detail_id = cd.id and cp.status = 'ACTIVE'
                            join product_contract.contracts c on cd.contract_id = c.id
                            join action.actions a on a.product_contract_id = c.id
                            join pod.pod_details pd on pd.id = cp.pod_detail_id
                                where a.id in :persistedActionIds
                                and cd.start_date >= coalesce((
                                    select max(start_date)
                                    from product_contract.contract_details cd1
                                        where cd1.contract_id = cd.contract_id
                                        and start_date <= a.execution_date),cd.start_date
                            )
                    ) as tbl
                    """
    )
    Long countPodsNotCoveredByRespectiveAndFutureContractVersionsOfPersistedActions(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate,
            @Param("persistedActionIds") List<Long> persistedActionIds
    );


    @Query(
            nativeQuery = true,
            value = """
                    select count(distinct pd.pod_id)
                    from product_contract.contract_details cd
                    join product_contract.contract_pods cp on cp.contract_detail_id = cd.id and cp.status = 'ACTIVE'
                    join pod.pod_details pd on pd.id = cp.pod_detail_id
                        where cd.contract_id = :contractId
                            and cd.start_date >= coalesce((
                                select max(start_date)
                                from product_contract.contract_details cd1
                                    where cd1.contract_id = cd.contract_id
                                    and start_date <= :executionDate), cd.start_date
                            )
                    """
    )
    Long countPodsFromRespectiveAndFutureContractVersions(
            @Param("contractId") Long contractId,
            @Param("executionDate") LocalDate executionDate
    );
    @Query("""
            select new bg.energo.phoenix.model.documentModels.action.ActionPodModel(
                pod.identifier,
                pdd.additionalIdentifier
            )
            from ActionPod ap
            join PointOfDelivery pod on ap.podId = pod.id
            join ProductContractDetails pcd on pcd.id = :pcDetailId
            join ContractPods cp on cp.contractDetailId = pcd.id and cp.status = 'ACTIVE'
            join PointOfDeliveryDetails pdd on pdd.podId = pod.id and pdd.id = cp.podDetailId
            where ap.status = 'ACTIVE'
            and ap.actionId = :actionId
            """)
    List<ActionPodModel> fetchActionPodsForPenaltyDoc(Long actionId, Long pcDetailId);

}
