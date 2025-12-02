package bg.energo.phoenix.repository.pod.pod;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.pod.pod.*;
import bg.energo.phoenix.model.response.contract.action.ActionPodResponse;
import bg.energo.phoenix.model.response.pod.pod.PointOfDeliveryFilterResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PointOfDeliveryRepository extends JpaRepository<PointOfDelivery, Long> {
    @Query("""
            select pod from PointOfDelivery pod
            where pod.id in(:ids)
            and pod.status in(:statuses)
            """)
    List<PointOfDelivery> findPointOfDeliveryByIdInAndStatusIn(@Param("ids") List<Long> ids, @Param("statuses") List<PodStatus> statuses);

    @Query("""
            select pod from PointOfDelivery pod
            join PointOfDeliveryDetails podd on podd.podId = pod.id
            where podd.customerId = :customerId
            and pod.status = 'ACTIVE'
            """)
    List<PointOfDelivery> findPointOfDeliveryByCustomerId(@Param("customerId") Long customerId);

    boolean existsByIdentifierIgnoreCaseAndStatusIn(String identifier, List<PodStatus> statuses);

    Optional<PointOfDelivery> findByIdAndStatusIn(Long id, List<PodStatus> statuses);

    Optional<PointOfDelivery> findByIdentifierAndStatus(String identifier, PodStatus status);

    Optional<PointOfDelivery> findByIdentifierAndStatusIn(String identifier, List<PodStatus> statuses);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.pod.pod.PointOfDeliveryFilterResponse(
                     p.id,
                     pd2.id,
                     p.identifier,
                     pd2.type,
                     c.identifier,
                     go2.name,
                     pd2.providedPower,
                     pd2.consumptionPurpose,
                     pd2.measurementType,
                     p.disconnectionPowerSupply,
                     p.status,
                     cd.name,
                     cd.middleName,
                     cd.lastName,
                     lf.name)
                        from PointOfDelivery p
                        join PointOfDeliveryDetails pd2 on p.lastPodDetailId = pd2.id
                        join GridOperator go2 on p.gridOperatorId = go2.id
                        left join Customer c on c.id = pd2.customerId
                        left join CustomerDetails cd on cd.id=c.lastCustomerDetailId
                        left join LegalForm lf on lf.id=cd.legalFormId
                        where p.id in (
                            select p2.id from PointOfDelivery p2
                            join PointOfDeliveryDetails pd on pd.podId = p2.id
                            where p2.status in :statuses
                            and ((:podTypes) is null or pd.type in (:podTypes))
                            and ((:gridOperatorIds) is null or p2.gridOperatorId  in (:gridOperatorIds))
                            and ((:consumptionPurposes) is null or  pd.consumptionPurpose in (:consumptionPurposes))
                            and ((:voltageLevels) is null or pd.voltageLevel in (:voltageLevels))
                            and ((:measurementTypes) is null or pd.measurementType in (:measurementTypes))
                            and (:disconnectionState is null or p2.disconnectionPowerSupply = :disconnectionState)
                            and (:providedPowerFrom is null or pd.providedPower >= :providedPowerFrom)
                            and (:providedPowerTo is null or pd.providedPower <= :providedPowerTo)
                            and (coalesce(:excludeOldVersions,'false') = 'false' or (:excludeOldVersions = 'true' and pd.id = p.lastPodDetailId))
                            and (:searchBy is null or (
                                 :searchBy =  'ALL' and (
                                     lower(pd.name) like :prompt
                                     or lower(pd.additionalIdentifier) like :prompt
                                     or lower(p2.identifier) like :prompt
                                     or exists (
                                         select 1 from Customer c
                                             where c.id = pd.customerId
                                             and lower(c.identifier) like :prompt )
                                 )
                                 or (
                                     (:searchBy = 'NAME' and lower(pd.name) like :prompt)
                                     or (:searchBy = 'ADDITIONALIDENTIFIER' and lower(pd.additionalIdentifier) like :prompt)
                                     or (:searchBy = 'IDENTIFIER' and lower(p2.identifier) like :prompt)
                                     or (:searchBy = 'CUSTOMERIDENTIFIER' and exists(
                                         select 1 from Customer c
                                         where c.id = pd.customerId
                                         and lower(c.identifier) like :prompt)
                                     )
                                 )
                            )
                            )
                        )
                    """
    )
    Page<PointOfDeliveryFilterResponse> list(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("podTypes") List<PODType> podTypes,
            @Param("gridOperatorIds") List<Long> gridOperatorIds,
            @Param("consumptionPurposes") List<PODConsumptionPurposes> consumptionPurposes,
            @Param("voltageLevels") List<PODVoltageLevels> voltageLevels,
            @Param("measurementTypes") List<PODMeasurementType> measurementTypes,
            @Param("providedPowerFrom") BigDecimal providedPowerFrom,
            @Param("providedPowerTo") BigDecimal providedPowerTo,
            @Param("excludeOldVersions") String excludeOldVersions,
            @Param("statuses") List<PodStatus> statuses,
            @Param("disconnectionState") Boolean disconnectionState,
            PageRequest pageRequest
    );

    @Query("""
                select new bg.energo.phoenix.model.CacheObject(p.id, p.identifier)
                from PointOfDelivery p
                where p.identifier = :identifier
                and p.status =:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getCacheObjectByIdentifierAndStatus(@Param("identifier") String identifier, @Param("status") PodStatus podStatus);

    @Query(
            value = """
                    select count(pod.id) > 0 from PointOfDelivery pod
                    join DiscountPointOfDeliveries dpod on dpod.pointOfDeliveryId = pod.id
                    join Discount d on d.id = dpod.discountId
                        where pod.id = :id
                        and d.status = 'ACTIVE'
                        and pod.status = 'ACTIVE'
                        and dpod.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToDiscount(@Param("id") Long id);

    @Query(
            value = """
                    select count(pod.id) > 0 from PointOfDelivery pod
                    join Meter m on m.podId = pod.id
                        where pod.id = :id
                        and m.status = 'ACTIVE'
                        and pod.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToMeter(@Param("id") Long id);

    @Query(
            value = """
                    select count(pod.id) > 0 from PointOfDelivery pod
                    join BillingByProfile bbp on bbp.podId = pod.id
                        where pod.id = :id
                        and bbp.status = 'ACTIVE'
                        and pod.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToBillingDataByProfile(@Param("id") Long id);

    @Query(
            value = """
                    select count(pod.id) > 0 from PointOfDelivery pod
                    join BillingByScale bbs on bbs.podId = pod.id
                        where pod.id = :id
                        and bbs.status = 'ACTIVE'
                        and pod.status = 'ACTIVE'
                    """
    )
    boolean hasActiveConnectionToBillingDataByScales(@Param("id") Long id);

    @Query("select pod.id from PointOfDelivery pod where pod.status = 'ACTIVE' and pod.id in :ids")
    List<Long> findByStatusActiveAndIdIn(@Param("ids") List<Long> ids);

    @Query("""
            select pod.identifier from PointOfDelivery pod
            join PointOfDeliveryDetails podd on pod.id = podd.podId
            join ContractPods cp on cp.podDetailId = podd.id
            join ProductContractDetails pcd on cp.contractDetailId = pcd.id
            join Contract c on c.id = pcd.contractId
            where cp.podDetailId = :podDetailId
            and cp.contractDetailId = :contractDetailId
            and pod.status = 'ACTIVE'
            and cp.status = 'ACTIVE'
            and c.status = 'ACTIVE'
            """)
    Optional<String> getIdentifierByProductContractPodDetailId(@Param("podDetailId") Long podDetailId, @Param("contractDetailId") Long contractDetailId);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.action.ActionPodResponse(
                        pod.id,
                        pod.identifier
                    )
                    from PointOfDelivery pod
                    where pod.identifier in :identifierPrompts
                    and pod.status = 'ACTIVE'
                    and exists(
                        select 1 from ContractPods cp
                        join PointOfDeliveryDetails pod2 on pod2.id = cp.podDetailId
                        join ProductContractDetails pcd on pcd.id = cp.contractDetailId
                        join ProductContract pc on pc.id = pcd.contractId
                        where pc.id = :contractId
                        and pod2.podId = pod.id
                        and cp.status = 'ACTIVE'
                        and pc.status = 'ACTIVE'
                    )
                    """
    )
    List<ActionPodResponse> searchPodsForAction(
            @Param("identifierPrompts") List<String> identifierPrompts,
            @Param("contractId") Long contractId
    );

    @Query(
            nativeQuery = true,
            value = """
                    select p.id from pod.pod p
                    join pod.pod_details pd on pd.pod_id = p.id
                    join product_contract.contract_pods cp on cp.pod_detail_id = pd.id
                    join product_contract.contract_details pcd on pcd.id = cp.contract_detail_id
                    join product_contract.contracts pc on pc.id = pcd.contract_id
                        where (
                            pcd.id = :contractDetailId
                            or pcd.start_date > (
                                select pcd1.start_date
                                    from product_contract.contract_details pcd1
                                    where pcd1.id = :contractDetailId
                            )
                        )
                        and pc.id = :contractId
                        and cp.status = 'ACTIVE'
                        and p.id in :podIds
                    """
    )
    List<Long> findPodsBelongingToContractForAction(
            @Param("contractId") Long contractId,
            @Param("contractDetailId") Long contractDetailId,
            @Param("podIds") List<Long> podIds
    );

    @Query(
            value = "select p.identifier from PointOfDelivery p where p.id in :podIds"
    )
    List<String> findIdentifiersByIdIn(@Param("podIds") List<Long> ids);

    @Query("""
            select count(pcd.id) > 0
            from PointOfDelivery pod
            join PointOfDeliveryDetails podd on podd.podId = pod.id
            join ContractPods cp on cp.podDetailId = podd.id
            join ProductContractDetails pcd on cp.contractDetailId = pcd.id
            join ProductContract pc on pcd.contractId = pc.id
            where pod.id = :id
            and cp.status = 'ACTIVE'
            and pc.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToProductContract(Long id);

    @Query("""
            select count(scp.id) > 0
            from PointOfDelivery pod
            join ServiceContractPods scp on scp.podId = pod.id
            join ServiceContractDetails scd on scp.contractDetailId = scd.id
            join ServiceContracts sc on sc.id = scd.contractId
            where pod.id = :id
            and scp.status = 'ACTIVE'
            and sc.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToServiceContract(Long id);

    @Query("""
            select count(so.id) > 0
            from PointOfDelivery pod
            join ServiceOrderPod sop on sop.podId = pod.id
            join ServiceOrder so on sop.orderId = so.id
            where pod.id = :id
            and sop.status = 'ACTIVE'
            and so.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToServiceOrder(Long id);

    @Query("""
            select count(a.id) > 0
            from PointOfDelivery pod
            join ActionPod ap on ap.podId = pod.id
            join Action a on a.id = ap.actionId
            where pod.id = :id
            and ap.status = 'ACTIVE'
            and a.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToAction(Long id);

    @Query("""
            select pod
            from PointOfDelivery pod
            join PointOfDeliveryDetails podd on podd.podId = pod.id
            join ContractPods cp on cp.podDetailId = podd.id
            where cp.id = :contractPodId
            """)
    Optional<PointOfDelivery> findByContractPodId(@Param("contractPodId") Long contractPodId);

    PointOfDelivery findByLastPodDetailIdAndStatus(Long podDetailId, PodStatus status);

    @Query("""
            SELECT DISTINCT p FROM PointOfDelivery p
                WHERE p.id IN (
                    SELECT DISTINCT i.podId
                    FROM Invoice i
                    WHERE i.id = :invoiceId
                    AND i.podId IS NOT NULL
                )
                OR p.id IN (
                    SELECT DISTINCT isdd.podId
                    FROM InvoiceStandardDetailedData isdd
                    WHERE isdd.invoiceId = :invoiceId
                    AND isdd.podId IS NOT NULL
                )
            """)
    List<PointOfDelivery> findDistinctPodsByInvoiceId(@Param("invoiceId") Long invoiceId);
}
