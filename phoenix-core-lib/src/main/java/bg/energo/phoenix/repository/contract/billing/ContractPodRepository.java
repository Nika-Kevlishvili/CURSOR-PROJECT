package bg.energo.phoenix.repository.contract.billing;

import bg.energo.phoenix.model.documentModels.contract.response.PodResponse;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetailsMapWithPods;
import bg.energo.phoenix.model.request.contract.pod.ActionFilterModel;
import bg.energo.phoenix.model.request.contract.pod.ActivationFilteredModel;
import bg.energo.phoenix.model.request.contract.pod.DeactivationFilteredModel;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieDealDatesUpdate.XEnergieGeneratorDealDatesUpdateModel;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationDataModel;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationFetchDataModel;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitCreationCommitment.SplitCreationCommitmentModel;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitUpdate.UpdateJobModel;
import org.springframework.data.domain.PageRequest;
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
public interface ContractPodRepository extends JpaRepository<ContractPods, Long> {

    Optional<ContractPods> findByBillingGroupIdAndPodDetailIdAndStatusIn(Long billingGroupId, Long podDetailId, List<EntityStatus> statuses);

    @Query(value = """
               select tbl.identifier,
                   tbl.podDetailName,
                   tbl.podDetailId,
                   tbl.versionId,
                   tbl.podId,
                    tbl.podType,
                    tbl.gridOperatorName,
                    tbl.podConsumptionPurpose,
                    tbl.podMeasurementType,
                    tbl.billingGroup,
                    tbl.podVersionCreateDate,
                    tbl.activationDate,
                    tbl.deactivationDate,
                    tbl.deactivationReason,
                    tbl.contractPodId,
                    tbl.rowNumber,
                    tbl.billingGroupId,
                    tbl.estimatedMonthlyAvgConsumption,
                    tbl.contractNumber
            from
                       (select
                            p.identifier as identifier,
                            pd.name as podDetailName,
                            pd.id as podDetailId,
                            pd.version_id as versionId,
                            p.id as podId,
                            pd.type as podType,
                            pd.estimated_monthly_avg_consumption as estimatedMonthlyAvgConsumption,
                            grop.name as gridOperatorName,
                            pd.consumption_purpose as podConsumptionPurpose,
                            pd.measurement_type as podMeasurementType,
                            (select group_number from product_contract.contract_billing_groups cbg where contract_id = :contractId order by id limit 1) as billingGroup,
                            (select id from product_contract.contract_billing_groups cbg where contract_id = :contractId order by id limit 1) as billingGroupId,
                            pd.create_date as podVersionCreateDate,
                            cp.activation_date as activationDate,
                            cp.deactivation_date as deactivationDate,
                            dp.name as deactivationReason,
                            cp.id as contractPodId,
                            c.contract_number as contractNumber,
                            row_number() over (partition by pd.pod_id order by pd.create_date desc) as rowNumber
                        from product_contract.contracts c
                        join product_contract.contract_details cd on cd.contract_id = c.id
                        join product_contract.contract_pods cp on cp.contract_detail_id = cd.id
                        join pod.pod_details pd on cp.pod_detail_id = pd.id
                        join pod.pod p on pd.pod_id = p.id
                        join nomenclature.grid_operators grop on p.grid_operator_id = grop.id
                        left join nomenclature.deactivation_purposes dp on cp.deactivation_purpose_id =  dp.id
                        where
                                cd.customer_detail_id in
                                (select id from customer.customer_details where customer_id = :customerId)
                           and
                                    (coalesce(:contractId,0) = 0 or c.id <> :contractId)
                          and
                            (:contractStatus = 'ALL'
                                or
                             (:contractStatus = 'ACTIVE'
                             and
                               c.status = 'ACTIVE'
                                   and
                               c.contract_status in ('READY', 'SIGNED', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
                                   and
                               current_date between cp.activation_date and coalesce(cp.deactivation_date,current_date)
                                   and
                               p.status =  'ACTIVE')
                           )
                       ) as tbl
                   where tbl.rowNumber = 1;                                       
            """
            , nativeQuery = true)
    List<ContractPodsResponse> getContractPodsByCustomerIdContractIdAndStatus(Long customerId, Long contractId, String contractStatus);


    @Query(value = """
            select tbl.identifier,
                   pd.name                              as podDetailName,
                   tbl.podDetailId,
                   pd.version_id                        as versionId,
                   pd.type                              as podType,
                   grop.name                            as gridOperatorName,
                   pd.consumption_purpose               as podConsumptionPurpose,
                   tbl.billingGroup,
                   pd.create_date                       as podVersionCreateDate,
                   tbl.activationDate,
                   tbl.deactivationDate,
                   dp.name                              as deactivationReason,
                   tbl.contractPodId,
                   tbl.contractdetailid,
                   tbl.contractid,
                   tbl.rn as rowNumber,
                   tbl.billingGroupId,
                   pd.estimated_monthly_avg_consumption as estimatedMonthlyAvgConsumption,
                   tbl.contract_number,
                   tbl.podid
            from (select p.identifier,
                         p.last_pod_detail_id                                              as podDetailId,
                         p.grid_operator_id,
                         (select group_number
                          from product_contract.contract_billing_groups cbg
                          where contract_id = c.id
                          order by id
                          limit 1)                                                         as billingGroup,
                         cp1.activation_Date                                               as activationDate,
                         cp1.deactivation_Date                                             as deactivationDate,
                         row_number() over (partition by p.id order by c.create_date desc) as rn,
                         cd.id                                                             as contractdetailid,
                         c.id                                                              as contractid,
                         cp1.deactivation_purpose_id,
                         cp1.id                                                            as contractPodId,
                         (select id
                          from product_contract.contract_billing_groups cbg
                          where contract_id = c.id
                          order by id
                          limit 1)                                                         as billingGroupId,
                         c.contract_number,
                         p.id                                                              as podid
                  from pod.pod p
                           join pod.pod_details pd
                                on pd.pod_id = p.id
                           join product_contract.contract_pods cp
                                on cp.pod_detail_id = pd.id
                           join product_contract.contract_details cd
                                on cp.contract_detail_id = cd.id
                           join product_contract.contracts c
                                on cd.contract_id = c.id
                           join customer.customer_details cdet
                                on cd.customer_detail_id = cdet.id
                                    and cdet.customer_id = :customerId
                           left join product_contract.contract_pods cp1
                                     on cp1.pod_detail_id = pd.id
                                         and cd.start_date =
                                             (select max(start_date)
                                              from product_contract.contract_details cdd
                                              where cdd.contract_id
                                                  = c.id
                                                and cdd.start_date <= current_date)) as tbl
                     join nomenclature.grid_operators grop on tbl.grid_operator_id = grop.id
                     left join nomenclature.deactivation_purposes dp on tbl.deactivation_purpose_id = dp.id
                     join pod.pod_details pd on pd.id = tbl.podDetailId
            where rn = 1;                                   
            """
            , nativeQuery = true)
    List<ContractPodsResponse> getAllContractPodsByCustomerId(@Param("customerId") Long customerId);

    boolean existsByBillingGroupIdAndStatusIn(Long groupId, List<EntityStatus> statuses);

    @Query("""
            select coalesce(max('true'), 'false') as have_future_activation_date
            from ContractPods cp
            join ProductContractDetails cd on cp.contractDetailId = cd.id and cp.status = 'ACTIVE'
            join ProductContract c on cd.contractId = c.id and c.status = 'ACTIVE'
            join PointOfDeliveryDetails podd on cp.podDetailId = podd.id
            where podd.podId = :pointOfDeliveryId
            and c.id <> :contractId
            and cp.activationDate > :signingDate
            """)
    boolean havePointOfDeliveryFutureActivationDate(@Param("pointOfDeliveryId") Long pointOfDeliveryId,
                                                    @Param("contractId") Long contractId,
                                                    @Param("signingDate") LocalDate signingDate);

    @Query("""
            select new bg.energo.phoenix.model.entity.contract.product.ProductContractDetailsMapWithPods(pod, pcd, cp)
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on cp.contractDetailId = pcd.id
            where pcd.contractId = :contractId
            and pcd.versionStatus = 'SIGNED'
            and cp.status in(:statuses)
            """)
    List<ProductContractDetailsMapWithPods> findAllByContractIdAndStatusInMergeWithPods(@Param("contractId") Long contractId,
                                                                                        @Param("statuses") List<EntityStatus> statuses);

    List<ContractPods> findAllByContractDetailIdAndStatusIn(Long contractDetailId, List<EntityStatus> statuses);

    @Query("""
            select cp
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            where cp.status in (:statuses)
            and podd.type = 'CONSUMER'
            and cp.contractDetailId = :contractDetailId
            """)
    List<ContractPods> findAllConsumersByContractDetailIdAndStatusIn(Long contractDetailId, List<EntityStatus> statuses);

    @Query("""
            select cp
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join ProductContractDetails pcd on cp.contractDetailId = pcd.id
            where cp.status in (:statuses)
            and pcd.versionStatus = 'SIGNED'
            and podd.type = 'GENERATOR'
            and cp.activationDate is not null
            and coalesce(cp.dealNumber, '') = ''
            and cp.contractDetailId = :contractDetailId
            """)
    List<ContractPods> findAllActivatedGeneratorsByContractDetailIdAndStatusInWithoutDeal(Long contractDetailId, List<EntityStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl(
            p.identifier,
            pd.name,
            pd.id,
            pd.versionId,
            p.id,
            pd.type,
            go.name,
            pd.consumptionPurpose,
            pd.measurementType,
            cbg.groupNumber,
            pd.createDate,
            cp.activationDate,
            cp.deactivationDate,
            dp.name,
            cp.id,
            cbg.id,
            pd.estimatedMonthlyAvgConsumption,
            dp.id,
            cp.dealNumber
            )
            from ContractPods cp
            join ContractBillingGroup cbg on cbg.id=cp.billingGroupId
            join PointOfDeliveryDetails pd on pd.id =cp.podDetailId
            join PointOfDelivery  p on p.id=pd.podId
            join GridOperator go on go.id=p.gridOperatorId
            left join DeactivationPurpose dp on dp.id=cp.deactivationPurposeId
            where cp.contractDetailId = :contractDetailId
            and cp.status in :statuses
            """)
    List<ContractPodsResponseImpl> getResponseByContractDetailIdAndStatusIn(Long contractDetailId, List<EntityStatus> statuses);

    Optional<ContractPods> findByContractDetailIdAndPodDetailIdAndStatusIn(Long contractDetailId, Long podDetailId, List<EntityStatus> statuses);

    @Query("""
            select cp
             from ContractPods cp
              join PointOfDeliveryDetails pd
              on cp.podDetailId = pd.id
              join
               ProductContractDetails cd2
               on cp.contractDetailId = cd2.id
              where pd.podId in (select pd2.podId from PointOfDeliveryDetails pd2 where pd2.id = :podDetailId)
              and cp.status = 'ACTIVE'
              and cp.activationDate is null
              and cd2.startDate > (
            select cd.startDate
             from ProductContractDetails cd
             where cd.id = :contractDetailId)
            and cp.contractDetailId  in(
            select cd.id
             from ProductContractDetails cd
             where cd.contractId in(select cd1.contractId
                                   from ProductContractDetails cd1
                                    where cd1.id = :contractDetailId))
            """)
    List<ContractPods> getContractPodsToDelete(Long contractDetailId, Long podDetailId);

    @Query("""
            select new bg.energo.phoenix.model.request.contract.pod.ActivationFilteredModel(
                pcd.id,
                pc.id,
                pcd.startDate,
                cp
            )
            from  ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract pc on pc.id = pcd.contractId
            where cp.status='ACTIVE'
            and cp.podDetailId in (
                select pd.id
                from PointOfDeliveryDetails pd
                where pd.podId = (
                    select pd2.podId
                    from PointOfDeliveryDetails pd2
                    where pd2.id = :podDetailId
                )
            )
            and (
                pc.contractStatus = 'SIGNED'
                and pc.subStatus in('SIGNED_BY_BOTH_SIDES','SPECIAL_PROCESSES')
                or pc.contractStatus in ('ENTERED_INTO_FORCE','ACTIVE_IN_TERM','ACTIVE_IN_PERPETUITY')
            )
            order by cp.activationDate
            """)
    List<ActivationFilteredModel> findAllContractPodsForPodWithPodDetailId(Long podDetailId);

    @Query("""
            select new bg.energo.phoenix.model.request.contract.pod.DeactivationFilteredModel(pcd.id,pc.id,pcd.startDate,cp,pc.contractNumber,pcd.versionId) 
            from  ContractPods cp 
            join ProductContractDetails pcd on pcd.id =cp.contractDetailId
            join ProductContract pc on pc.id=pcd.contractId
            where cp.podDetailId in (
            select pd.id from PointOfDeliveryDetails pd 
            where pd.podId = :podId
            )
            and cp.status='ACTIVE'
            and pcd.versionStatus='SIGNED'
            order by cp.activationDate
            """)
    List<DeactivationFilteredModel> findAllContractPodsForPodWithPodId(Long podId);

    @Query(value = """
            select case when count(cp.id) > 0 then true else false  end 
            from product_contract.contract_pods cp
            join product_contract.contract_details pcd on pcd.id=cp.contract_detail_id
            join product_contract.contracts pc on pc.id = pcd.contract_id
            join pod.pod_details pd on pd.id=cp.pod_detail_id
            join pod.pod p on p.id =pd.pod_id
            where cp.status='ACTIVE'
            and cp.activation_date is not null
            and cp.deactivation_date is null
            and (select pcd2.start_date from product_contract.contract_details pcd2 where pcd2.contract_id=pc.id and pcd2.start_date> pcd.start_date order by pcd2.start_date  limit 1) < :activationDate
            and p.id = :podId
            and pc.id = :contractId
            """, nativeQuery = true)
    boolean checkIfPodIsActiveInAnyContract(@Param("activationDate") LocalDate currentActivationDate,
                                            @Param("podId") Long podId,
                                            @Param("contractId") Long contractId);

    @Query("select count(cp.id) from ContractPods as cp where cp.contractDetailId = :contractDetailId and cp.status = :status")
    Long countByContractDetailIdAndStatus(@Param("contractDetailId") Long contractDetailId,
                                          @Param("status") EntityStatus status);

    @Query(value = """
            select cd.start_date from product_contract.contract_details cd 
            where cd.start_date>:startDate
            and cd.contract_id = :contractId
            order by cd.start_date
            limit 1
            """, nativeQuery = true)
    Optional<LocalDate> findNextStartDate(LocalDate startDate, Long contractId);

    Long countByContractDetailIdInAndStatusInAndActivationDateNotNull(List<Long> contractDetailIds, List<EntityStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.request.contract.pod.ActionFilterModel(
                pcd.id,
                pcd.contractId,
                c.contractNumber,
                pcd.versionId,
                pcd.startDate,
                cp,
                pcd.customerDetailId,
                pd2.podId,
                cd.customerId
            )
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join CustomerDetails cd on cd.id = pcd.customerDetailId
            join PointOfDeliveryDetails pd2 on pd2.id = cp.podDetailId
            join Contract c on c.id = pcd.contractId
            where cp.podDetailId in (
                select pd.id from PointOfDelivery p
                join PointOfDeliveryDetails pd on pd.podId = p.id
                and p.identifier = :podIdentifier
                and p.status = 'ACTIVE'
            )
            and cp.status='ACTIVE'
            """)
    List<ActionFilterModel> findByPodIdentifier(String podIdentifier);

    @Query("""
            select cp
            from ContractPods cp
            join ProductContractDetails pcd on cp.contractDetailId = pcd.id
            join Contract c on c.id = pcd.contractId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            where cp.status = 'ACTIVE'
            and c.id = :contractId
            and pod.id = :podId
            and pod.status = 'ACTIVE'
            and cp.activationDate >= :resigningDate
            order by cp.activationDate
            """)
    List<ContractPods> findFutureContractPodsForResigning(@Param("contractId") Long contractId,
                                                          @Param("podId") Long podId,
                                                          @Param("resigningDate") LocalDate resigningDate);

    @Query("""
            select cp from ContractPods as cp
            where cp.contractDetailId = :contractDetailId
            and cp.status = :status
            and cp.activationDate is not null
            """)
    List<ContractPods> getPodsThatHaveActivationDate(@Param("contractDetailId") Long contractDetailId,
                                                     @Param("status") EntityStatus status);

    @Query("""
            select cp
            from ContractPods as cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract pc on pcd.contractId = pc.id
            where pc.id = :contractId
            and cp.status = :status
            and cp.activationDate is not null
            """)
    List<ContractPods> getPodsThatHaveActivationDateInContractAnyVersion(@Param("contractId") Long contractId,
                                                                         @Param("status") EntityStatus status);

    @Query("""
            select distinct pcd.dealNumber
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            where coalesce(pcd.dealNumber, '') <> ''
            and (cp.customModifyDate between :yesterdayStart and :yesterdayEnd)
            and cp.status = 'ACTIVE'
            and cp.contractDetailId is not null
            and podd.type = 'CONSUMER'
            """)
    List<String> findDealsByConsumerPointOfDeliveriesAndModifyDatePastOneDay(@Param("yesterdayStart") LocalDateTime start,
                                                                             @Param("yesterdayEnd") LocalDateTime end,
                                                                             PageRequest pageRequest);

    @Query("""
            select count(distinct pcd.dealNumber)
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            where coalesce(pcd.dealNumber, '') <> ''
            and (cp.customModifyDate between :yesterdayStart and :yesterdayEnd)
            and cp.status = 'ACTIVE'
            and cp.contractDetailId is not null
            and podd.type = 'CONSUMER'
            """)
    Long countDealsByModifyDatePastOneDay(@Param("yesterdayStart") LocalDateTime start,
                                          @Param("yesterdayEnd") LocalDateTime end);

    @Query("""
            select new bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieDealDatesUpdate.XEnergieGeneratorDealDatesUpdateModel(
                pc,
                pcd,
                cp
            )
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract pc on pc.id = pcd.contractId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            where coalesce(cp.dealNumber, '') <> ''
            and (cp.customModifyDate between :yesterdayStart and :yesterdayEnd)
            and cp.status = 'ACTIVE'
            and cp.contractDetailId is not null
            and podd.type = 'GENERATOR'
            """)
    List<XEnergieGeneratorDealDatesUpdateModel> findDealsByGeneratorPointOfDeliveriesAndModifyDatePastOneDay(@Param("yesterdayStart") LocalDateTime start,
                                                                                                             @Param("yesterdayEnd") LocalDateTime end,
                                                                                                             PageRequest pageRequest);

    @Query("""
            select count(cp.id)
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            where coalesce(cp.dealNumber, '') <> ''
            and (cp.customModifyDate between :yesterdayStart and :yesterdayEnd)
            and cp.status = 'ACTIVE'
            and cp.contractDetailId is not null
            and podd.type = 'GENERATOR'
            """)
    Long countDealsByGeneratorPointOfDeliveriesAndModifyDatePastOneDay(@Param("yesterdayStart") LocalDateTime start,
                                                                       @Param("yesterdayEnd") LocalDateTime end);

    @Query("""
            select cp
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            where cp.customModifyDate >= :yesterdayStart
            and pcd.dealNumber is not null
            and cp.customModifyDate <= :yesterdayEnd
            and cp.status = 'ACTIVE'
            and cp.contractDetailId is not null
            """)
    List<ContractPods> findDealsByModifyDatePastOneDayTestControllerOnly(@Param("yesterdayStart") LocalDateTime start,
                                                                         @Param("yesterdayEnd") LocalDateTime end);

    @Query("""
            select cp
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            where pcd.dealNumber = :dealNumber
            and cp.status = 'ACTIVE'
            and podd.type = 'CONSUMER'
            """)
    List<ContractPods> findContractPodsByProductContractDealNumber(@Param("dealNumber") String dealNumber);

    @Query("""
            select distinct concat(pc.contractNumber, ' ', pod.identifier)
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract pc on pc.id = pcd.contractId
            where cp.dealNumber = :dealNumber
            and cp.status in(:statuses)
            and pc.status = 'ACTIVE'
            and podd.type = 'GENERATOR'
            """)
    List<String> findGeneratorProductContractPointOfDeliveriesWithDealNumber(@Param("dealNumber") String dealNumber,
                                                                             @Param("statuses") List<EntityStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitUpdate.UpdateJobModel(
                cp.id,
                cp.customModifyDate,
                cp.splitId,
                cp.activationDate,
                cp.deactivationDate,
                cp.podDetailId,
                case when podd.type = 'CONSUMER' then pcd.dealNumber else cp.dealNumber end,
                pod.gridOperatorId,
                pod.identifier
            )
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            where cp.activationDate is not null
            and (coalesce(pcd.dealNumber, '') <> '' or coalesce(cp.dealNumber, '') <> '')
            and cp.splitId is null
            """)
    List<UpdateJobModel> findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergie(PageRequest pageRequest);


    @Query("""
            select count(cp.id)
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            where cp.activationDate is not null
            and (coalesce(pcd.dealNumber, '') <> '' or coalesce(cp.dealNumber, '') <> '')
            and cp.splitId is null
            """)
    Long countAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergie();

    @Query("""
            select new bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitUpdate.UpdateJobModel(
                cp.id,
                cp.customModifyDate,
                cp.splitId,
                cp.activationDate,
                cp.deactivationDate,
                cp.podDetailId,
                case when podd.type = 'CONSUMER' then pcd.dealNumber else cp.dealNumber end,
                pod.gridOperatorId,
                pod.identifier
            )
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            where cp.splitId is not null
            and cp.customModifyDate between :yesterdayStart and :yesterdayEnd
            """)
    List<UpdateJobModel> findAllUpdatedProductContractPointOfDeliveriesThatSynchronizedInXEnergie(LocalDateTime yesterdayStart, LocalDateTime yesterdayEnd, PageRequest pageRequest);

    @Query("""
            select count(cp.id)
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            where cp.splitId is not null
            and cp.customModifyDate between :yesterdayStart and :yesterdayEnd
            """)
    Long countAllUpdatedProductContractPointOfDeliveriesThatSynchronizedInXEnergie(LocalDateTime yesterdayStart, LocalDateTime yesterdayEnd);

    @Query("""
            select distinct new bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationFetchDataModel(
                pod.id,
                pod.identifier,
                coalesce(pgo.ownedByEnergoPro, false)
            )
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join GridOperator pgo on pgo.id = pod.gridOperatorId
            join CustomerDetails cd on cd.id = pcd.customerDetailId
            join Customer c on c.id = cd.customerId
            where cp.activationDate is not null
            and coalesce(pcd.dealNumber, '') <> ''
            and cp.splitId is null
            and cp.status = 'ACTIVE'
            and pod.status = 'ACTIVE'
            and cp.customModifyDate >= :dateBefore
            """)
    List<ExcelGenerationFetchDataModel> findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate(
            @Param("dateBefore") LocalDateTime dateBefore,
            PageRequest pageRequest
    );

    @Query("""
            select new bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationDataModel(
                pod.identifier,
                podd.additionalIdentifier,
                pod.identifier,
                cp.activationDate,
                cp.deactivationDate,
                pgo.codeForXEnergy,
                pgo.gridOperatorCode,
                c.customerNumber,
                pgo.codeForXEnergy,
                pcd.dealNumber,
                coalesce(pgo.ownedByEnergoPro, false)
            )
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join GridOperator pgo on pgo.id = pod.gridOperatorId
            join CustomerDetails cd on cd.id = pcd.customerDetailId
            join Customer c on c.id = cd.customerId
            where cp.activationDate is not null
            and (cast(:activationFrom as date) is null or cp.activationDate >= cast(:activationFrom as date))
            and (cast(:deactivationTo as date) is null or cp.deactivationDate <= cast(:deactivationTo as date))
            and coalesce(pcd.dealNumber, '') <> ''
            and cp.splitId is null
            and cp.status = 'ACTIVE'
            and pod.status = 'ACTIVE'
            and pod.id = :podId
            and cp.customModifyDate >= cast(:dateBefore as date)
            """)
    List<ExcelGenerationDataModel> findAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDateForEnergoProPointOfDelivery(
            @Param("podId") Long id,
            @Param("dateBefore") LocalDateTime dateBefore,
            @Param("activationFrom") LocalDateTime activationFrom,
            @Param("deactivationTo") LocalDateTime deactivationTo
    );

    @Query("""
            select distinct count(pod.identifier)
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join GridOperator pgo on pgo.id = pod.gridOperatorId
            join CustomerDetails cd on cd.id = pcd.customerDetailId
            join Customer c on c.id = cd.customerId
            where cp.activationDate is not null
            and coalesce(pcd.dealNumber, '') <> ''
            and cp.splitId is null
            and cp.status = 'ACTIVE'
            and pod.status = 'ACTIVE'
            and cp.customModifyDate >= :dateBefore
            """)
    Long countAllNonSynchronizedProductContractPointOfDeliveriesWithSplitsFromXEnergieBeforeDate(
            @Param("dateBefore") LocalDateTime dateBefore
    );

    @Query("""
            select count(cp.id) > 0
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract pc on pcd.contractId = pc.id
            where pc.id = :productContractId
            and cp.status = 'ACTIVE'
            and cp.activationDate is not null
            """)
    boolean existsAnyActiveFutureOrPresentPointOfDeliveryByProductContractId(
            @Param("productContractId") Long productContractId
    );

    @Query("""
            select count(cp.id)
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract c on c.id = pcd.contractId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            where pod.status = 'ACTIVE'
            and cp.activationDate is not null
            and coalesce(pcd.dealNumber, '') <> ''
            and cp.status = 'ACTIVE'
            and c.status = 'ACTIVE'
            and cp.customModifyDate >= :dateBefore
            """)
    Long countAllNonSynchronizedContractPods(@Param("dateBefore") LocalDateTime dateBefore);

    @Query("""
            select new bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieSplitCreationCommitment.SplitCreationCommitmentModel(
                cp.id,
                cp.activationDate,
                cp.deactivationDate,
                pod.identifier,
                case when podd.type = 'CONSUMER' then pcd.dealNumber
                    when podd.type = 'GENERATOR' then cp.dealNumber
                    else '' end
            )
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract c on c.id = pcd.contractId
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            where pod.status = 'ACTIVE'
            and cp.activationDate is not null
            and (coalesce(pcd.dealNumber, '') <> '' or coalesce(cp.dealNumber, '') <> '')
            and cp.status = 'ACTIVE'
            and c.status = 'ACTIVE'
            and cp.customModifyDate >= :dateBefore
            """)
    List<SplitCreationCommitmentModel> findAllNonSynchronizedContractPods(@Param("dateBefore") LocalDateTime dateBefore, PageRequest pageRequest);

    @Query("""
            select distinct c.contractNumber
            from ContractPods cp
            join PointOfDeliveryDetails podd on cp.podDetailId = podd.id
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join Contract c on c.id = pcd.contractId
            where c.status = 'ACTIVE'
            and (
                 c.id <> :productContractId
                 or (c.id = :productContractId and podd.podId <> :pointOfDeliveryId)
            )
            and (
                (
                    cp.dealNumber = :dealNumber and cp.status = 'ACTIVE'
                )
            )
            or pcd.dealNumber = :dealNumber
            """)
    List<String> findContractNumbersByPointOfDeliveryDealNumberOrProductContractDealNumber(@Param("productContractId") Long productContractId,
                                                                                           @Param("pointOfDeliveryId") Long pointOfDeliveryId,
                                                                                           @Param("dealNumber") String dealNumber);

    @Transactional
    @Modifying
    @Query("""
            update ContractPods cp set cp.splitId = :splitId where cp.id = :id
            """)
    void updateSplitId(@Param("id") Long id, @Param("splitId") Long splitId);


    @Query("""
            select count(cp.id)=0 from ContractPods cp
            join  PointOfDeliveryDetails  pdd on pdd.id = cp.podDetailId
            join ProductContractDetails pcd on pcd.id=cp.contractDetailId
            where pdd.podId = :podId
            and pcd.contractId = :contractId
            and pcd.startDate = :startDate
            and cp.status='ACTIVE'
            """)
    boolean checkPodExistInNextVersion(Long podId, Long contractId, LocalDate startDate);

    @Query("""
            select cp
            from ContractPods cp
            join ProductContractDetails pcd on pcd.id = cp.contractDetailId
            join ProductContract pc on pc.id = pcd.contractId
            where pc.id = :productContractId
            and cp.dealNumber = :dealNumber
            """)
    List<ContractPods> findAllGeneratorsByDealNumberInContract(Long productContractId, String dealNumber);

    @Query(value = """
            with pods AS (
                    select innerPD.pod_id as podId
                    from pod.pod_details innerPD
                    where innerPD.id = :podDetailId
            )
            select cp.*
            from pod.pod_details pd
            join pods on pd.pod_id = pods.podId and pd.type = 'GENERATOR'
            join product_contract.contract_pods cp on cp.pod_detail_id = pd.id
            and cp.contract_detail_id = :productContractDetailId
            and cp.status = 'ACTIVE'
            """, nativeQuery = true)
    Optional<ContractPods> findGeneratorProductContractPointOfDeliveryByPointOfDeliveryDetailId(Long productContractDetailId,
                                                                                                Long podDetailId);

    @Query("""
            select cp
            from ContractPods cp
            where cp.billingGroupId = :billingGroupId
            and cp.status = 'ACTIVE'
            """)
    List<ContractPods> findProductContractPointOfDeliveriesByBillingGroup(Long billingGroupId);

    @Query("""
            select distinct pod.id
            from ContractPods cp
            join PointOfDeliveryDetails podd on podd.id = cp.podDetailId
            join PointOfDelivery pod on pod.id = podd.podId
            where pod.status = 'ACTIVE'
            and cp.billingGroupId = :billingGroupId
            and :billingDate between cp.activationDate and coalesce(cp.deactivationDate, date('01-01-2090'))
            and cp.status = 'ACTIVE'
            """)
    List<Long> findProductContractPointOfDeliveriesByBillingGroupAndBillingDate(
            @Param("billingGroupId") Long billingGroupId,
            @Param("billingDate") LocalDate billingDate
    );

    @Query(nativeQuery = true, value = """
            with previous_version_pods as (select pod.identifier, cp.id
                                           from pod.pod_details pd
                                                    join pod.pod pod on pd.pod_id = pod.id
                                                    join product_contract.contract_pods cp
                                                         on pd.id = cp.pod_detail_id and cp.status = 'ACTIVE'
                                                    join product_contract.contract_details cd
                                                         on cp.contract_detail_id = cd.id and cd.version_id = :versionId - 1
                                                    join product_contract.contracts c on cd.contract_id = c.id and c.id = :id),
                 current_version_pods as (select pod.identifier, cp.id
                                          from pod.pod_details pd
                                                   join pod.pod pod on pd.pod_id = pod.id
                                                   join product_contract.contract_pods cp
                                                        on pd.id = cp.pod_detail_id and cp.status = 'ACTIVE'
                                                   join product_contract.contract_details cd
                                                        on cp.contract_detail_id = cd.id and cd.version_id = :versionId
                                                   join product_contract.contracts c on cd.contract_id = c.id and c.id = :id),
                 pods_all_info as (select cont_pd.id,
                                          pod.identifier,
                                          pd.additional_identifier,
                                          pd.type,
                                          replace(text(pd.consumption_purpose), '_', ' ') as consumption_purpose,
                                          replace(text(pd.measurement_type), '_', ' ')    as measurement_type,
                                          pd.estimated_monthly_avg_consumption,
                                          pd.name                                         as pd_name,
                                          go.name                                         as go_name,
                                          case
                                              when pd.foreign_address = true then pd.populated_place_foreign
                                              else pp.name end                            as populated_place,
                                          case
                                              when pd.foreign_address = true then pd.zip_code_foreign
                                              else zc.zip_code end                        as zip_code,
                                          case
                                              when pd.foreign_address = true then pd.district_foreign
                                              else distr.name end                         as district,
                                          case
                                              when pd.foreign_address = true
                                                  then replace(text(pd.foreign_residential_area_type), '_', ' ')
                                              else replace(text(ra.type), '_', ' ') end   as ra_type,
                                          case
                                              when pd.foreign_address = true then pd.residential_area_foreign
                                              else ra.name end                            as ra_name,
                                          case
                                              when pd.foreign_address = true then pd.foreign_street_type
                                              else str.type end                           as street_type,
                                          case
                                              when pd.foreign_address = true then pd.street_foreign
                                              else str.name end                           as street,
                                          pd.street_number,
                                          pd.block,
                                          pd.entrance,
                                          pd.floor,
                                          pd.apartment,
                                          pd.address_additional_info,
                                          case
                                              when pd.foreign_address = false then
                                                  concat_ws(', ',
                                                            nullif(distr.name, ''),
                                                            nullif(concat_ws(' ', replace(text(ra.type), '_', ' '), ra.name), ''),
                                                            nullif(concat_ws(' ', str.type, str.name, pd.street_number), ''),
                                                            nullif(concat('бл. ', pd.block), 'бл. '),
                                                            nullif(concat('вх. ', pd.entrance), 'вх. '),
                                                            nullif(concat('ет. ', pd.floor), 'ет. '),
                                                            nullif(concat('ап. ', pd.apartment), 'ап. '),
                                                            pd.address_additional_info
                                                  )
                                              else
                                                  concat_ws(', ',
                                                            nullif(pd.district_foreign, ''),
                                                            nullif(concat_ws(' ',
                                                                             replace(text(pd.foreign_residential_area_type), '_', ' '),
                                                                             pd.residential_area_foreign), ''),
                                                            nullif(
                                                                    concat_ws(' ', pd.foreign_street_type, pd.street_foreign,
                                                                              pd.street_number),
                                                                    ''),
                                                            nullif(concat('бл. ', pd.block), 'бл. '),
                                                            nullif(concat('вх. ', pd.entrance), 'вх. '),
                                                            nullif(concat('ет. ', pd.floor), 'ет. '),
                                                            nullif(concat('ап. ', pd.apartment), 'ап. '),
                                                            pd.address_additional_info
                                                  )
                                              end                                         as formatted_address
                                   from pod.pod_details pd
                                            join pod.pod pod on pd.pod_id = pod.id
                                            join product_contract.contract_pods cont_pd
                                                 on pd.id = cont_pd.pod_detail_id and cont_pd.status = 'ACTIVE'
                                            left join nomenclature.districts distr on pd.district_id = distr.id
                                            left join nomenclature.zip_codes zc on pd.zip_code_id = zc.id
                                            left join nomenclature.residential_areas ra on pd.residential_area_id = ra.id
                                            left join nomenclature.streets str on pd.street_id = str.id
                                            left join nomenclature.populated_places pp on pd.populated_place_id = pp.id
                                            join nomenclature.grid_operators go on pod.grid_operator_id = go.id
                                            join product_contract.contract_details pcd on cont_pd.contract_detail_id = pcd.id
                                   where pcd.contract_id = :id)
            select cp.identifier                                                       as PODID,
                   cp.additional_identifier                                            as PODAdditionalID,
                   cp.pd_name                                                          as PODName,
                   translation.translate_text(cp.formatted_address, text('BULGARIAN')) as PODAddressComb,
                   cp.formatted_address                                                as PODAddressCombTrsl,
                   cp.populated_place                                                  as PODPlace,
                   cp.zip_code                                                         as PODZIP,
                   cp.type                                                             as PODType,
                   cp.go_name                                                          as PODGO,
                   cp.consumption_purpose                                              as PODConsumptionPurpose,
                   cp.measurement_type                                                 as PODMeasurementType,
                   cp.estimated_monthly_avg_consumption                                as EstimatedConsumption,
                   'ADDED'                                                             as PodState
            from current_version_pods p
                     join pods_all_info cp on p.id = cp.id
            where cp.identifier not in (select pvp.identifier from previous_version_pods pvp)
              and :versionId > 1
            union
            select cp.identifier                                                       as PODID,
                   cp.additional_identifier                                            as PODAdditionalID,
                   cp.pd_name                                                          as PODName,
                   translation.translate_text(cp.formatted_address, text('BULGARIAN')) as PODAddressComb,
                   cp.formatted_address                                                as PODAddressCombTrsl,
                   cp.populated_place                                                  as PODPlace,
                   cp.zip_code                                                         as PODZIP,
                   cp.type                                                             as PODType,
                   cp.go_name                                                          as PODGO,
                   cp.consumption_purpose                                              as PODConsumptionPurpose,
                   cp.measurement_type                                                 as PODMeasurementType,
                   cp.estimated_monthly_avg_consumption                                as EstimatedConsumption,
                   'REMOVED'                                                           as PodState
            from previous_version_pods p
                     join pods_all_info cp on p.id = cp.id
            where cp.identifier not in (select pvp.identifier from current_version_pods pvp)
            union
            select cp.identifier                                                       as PODID,
                   cp.additional_identifier                                            as PODAdditionalID,
                   cp.pd_name                                                          as PODName,
                   translation.translate_text(cp.formatted_address, text('BULGARIAN')) as PODAddressComb,
                   cp.formatted_address                                                as PODAddressCombTrsl,
                   cp.populated_place                                                  as PODPlace,
                   cp.zip_code                                                         as PODZIP,
                   cp.type                                                             as PODType,
                   cp.go_name                                                          as PODGO,
                   cp.consumption_purpose                                              as PODConsumptionPurpose,
                   cp.measurement_type                                                 as PODMeasurementType,
                   cp.estimated_monthly_avg_consumption                                as EstimatedConsumption,
                   'CURRENT'                                                           as PodState
            from current_version_pods p
                     join pods_all_info cp on p.id = cp.id
            """)
    List<PodResponse> fetchVersionPodsForDocument(Long id, Integer versionId);

}
