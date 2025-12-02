package bg.energo.phoenix.repository.contract.billing;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupNumberWrapper;
import bg.energo.phoenix.model.response.contract.biling.ContractBillingGroupResponse;
import bg.energo.phoenix.model.response.receivable.payment.ContractBillingGroupsForPaymentMiddleResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ContractBillingGroupRepository extends JpaRepository<ContractBillingGroup, Long> {
    @Query(nativeQuery = true, name = "product_contract.group_number_calculator")
    List<BillingGroupNumberWrapper> findAllBillingGroupNumberAvailableInPod(Long contractId);

    Optional<ContractBillingGroup> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);


    @Query("""
            select new bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse(cbg.id,cbg.groupNumber)
            from ContractBillingGroup cbg
            where cbg.contractId = :contractId
            and cbg.status in (:statuses)
            order by cbg.groupNumber
            """)
    List<BillingGroupListingResponse> findAllByContractId(@Param("contractId") Long contractId,
                                                          @Param("statuses") List<EntityStatus> statuses);

    @Query(nativeQuery = true,
            value = """
                    select distinct
                      cbg.id as id, cbg.group_number as groupNumber
                    from customer.customers c
                     join customer.customer_details cd
                       on cd.customer_id = c.id
                      and c.id =  :customerId
                     join product_contract.contract_details pcd
                       on pcd.customer_detail_id = cd.id
                     join product_contract.contracts pc
                       on pcd.contract_id = pc.id
                      and pc.status = 'ACTIVE'
                     join product_contract.contract_billing_groups cbg
                       on cbg.contract_id = pc.id
                      and cbg.status = 'ACTIVE'
                    """)
    List<ContractBillingGroupResponse> findAllByCustomerId(@Param("customerId") Long customerId);

    @Query(nativeQuery = true,
            value = """
                    SELECT
                        cbg.id AS id,
                        cbg.group_number AS groupNumber,
                        pc.id AS productContractId,
                        pcd.version_id AS productContractVersion
                    FROM customer.customers c
                             JOIN customer.customer_details cd
                                  ON cd.customer_id = c.id
                                      AND c.id = :customerId
                             JOIN product_contract.contract_details pcd
                                  ON pcd.customer_detail_id = cd.id
                             JOIN product_contract.contracts pc
                                  ON pcd.contract_id = pc.id
                                      AND pc.status = 'ACTIVE'
                             JOIN product_contract.contract_billing_groups cbg
                                  ON cbg.contract_id = pc.id
                                      AND cbg.status = 'ACTIVE'
                    WHERE cbg.group_number = COALESCE(:prompt, cbg.group_number)
                    ORDER BY cbg.id DESC;
                    """)
    Page<ContractBillingGroupsForPaymentMiddleResponse> findAllByCustomerIdForPayment(
            @Param("customerId") Long customerId,
            @Param("prompt") String prompt,
            Pageable pageable
    );

    Optional<ContractBillingGroup> findByContractIdAndGroupNumberAndStatusIn(Long contractId, String groupNumber, List<EntityStatus> statuses);

    Optional<ContractBillingGroup> findByContractIdAndIdAndStatusIn(Long contractId, Long id, List<EntityStatus> statuses);

    @Query("""
                    select max(bg.id)
                    from ContractBillingGroup bg
                    where bg.contractId = :contractId
                    and bg.status='ACTIVE'
            """)
    Optional<Long> findDefaultCacheObjectByContractId(Long contractId);

    @Query("""
                    select b.id
                    from ContractBillingGroup b
                    where b.id in :ids
                    and b.status= :entityStatus
            """)
    Set<Long> findByIds(Set<Long> ids, EntityStatus entityStatus);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse(cbg.id, cbg.groupNumber)
            from ContractBillingGroup cbg
            where cbg.id = :id
            and cbg.contractId = :contractId
            and cbg.status = :status
            """)
    Optional<BillingGroupListingResponse> findByIdAndContractIdAndStatus(@Param("id") Long billingGroupId,
                                                                         @Param("contractId") Long contractId,
                                                                         @Param("status") EntityStatus entityStatus);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse(cbg.id, cbg.groupNumber)
            from ContractBillingGroup cbg
            where cbg.id in :billingGroupIds
            and cbg.contractId = :contractId
            and cbg.status = :status
            """)
    Set<BillingGroupListingResponse> findAllIdAndContractIdAndStatus(@Param("billingGroupIds") List<Long> billingGroupIds,
                                                                     @Param("contractId") Long contractId,
                                                                     @Param("status") EntityStatus entityStatus);

    @Query("""
            select new bg.energo.phoenix.model.CacheObject(cbg.id,cbg.groupNumber)
            from ContractBillingGroup cbg
            where cbg.groupNumber=:groupNumber
            and cbg.status=:status
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findCacheObjectByName(String groupNumber, EntityStatus status);

    @Query("""
                    select distinct
                      new bg.energo.phoenix.model.CacheObject(cbg.id,cbg.groupNumber)
                    from Customer c
                     join CustomerDetails cd
                       on cd.customerId = c.id
                      and c.id =  :customerId
                     join ProductContractDetails pcd
                       on pcd.customerDetailId = cd.id
                     join ProductContract pc
                       on pcd.contractId = pc.id
                      and pc.status = 'ACTIVE'
                     join ContractBillingGroup cbg
                       on cbg.contractId = pc.id
                      and cbg.status = 'ACTIVE'
                       where cbg.groupNumber=:groupNumber
                    """)
    Optional<CacheObject> findByCustomerIdAndGroupNumber(@Param("customerId") Long customerId,@Param("groupNumber") String groupNumber);

    Optional<ContractBillingGroup> findByGroupNumberAndStatus(String groupNumber, EntityStatus status);

    @Query("""
                 select distinct new bg.energo.phoenix.model.CacheObject(cbg.id,cbg.groupNumber) from ContractBillingGroup cbg
                 join ProductContract pc on pc.id=cbg.contractId
                 join ProductContractDetails pcd on pcd.contractId = pc.id
                 join CustomerDetails cd on cd.id = pcd.customerDetailId
                 where cbg.groupNumber=:groupNumber
                 and cd.customerId=:customerId
                 and cbg.status=:status
            """)
    Optional<CacheObject> findCacheObjectByNameAndCustomerId(Long customerId, String groupNumber, EntityStatus status);
}
