package bg.energo.phoenix.repository.pod.pod;

import bg.energo.phoenix.model.CacheObjectForPod;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.response.pod.pod.PointOfDeliveryView;
import bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationLatestPointOfDeliveryDetailsDataModel;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PointOfDeliveryDetailsRepository extends JpaRepository<PointOfDeliveryDetails, Long> {

    Optional<PointOfDeliveryDetails> findByPodIdAndVersionId(Long podId, Integer versionId);

    @Query("select max(p.versionId) from PointOfDeliveryDetails p where p.podId = :podId")
    Optional<Integer> findMaxVersionId(@Param("podId") Long podId);

    @Query("""
            SELECT pd FROM PointOfDeliveryDetails pd WHERE pd.podId = :id AND pd.versionId = (SELECT MAX(pd2.versionId) FROM PointOfDeliveryDetails pd2 WHERE pd2.podId = :id)
            """)
    Optional<PointOfDeliveryDetails> findByPodId(Long id);

    List<PointOfDeliveryDetails> findAllByPodIdOrderByVersionIdAsc(Long podId);

    List<PointOfDeliveryDetails> findAllByPodIdOrderByVersionIdDesc(Long podId);

    @Query(
            value = """
                    select count(podd.id) > 0 from PointOfDeliveryDetails podd
                        where podd.podId = :podId
                        and podd.measurementType = 'SLP'
                    """
    )
    boolean hasPodMeasurementTypeSLP(
            @Param("podId") Long podId
    );


    @Query(
            value = """
                    select podd.estimatedMonthlyAvgConsumption from PointOfDeliveryDetails podd
                        where podd.id in :podDetailIds
                        and podd.type ='CONSUMER'
                    """
    )
    List<Integer> findEstimatedMonthlyAvgConsumptionByIdIn(List<Long> podDetailIds);


    @Query("""
            select new bg.energo.phoenix.model.response.pod.pod.PointOfDeliveryView(pd.podId,pd.id) from PointOfDeliveryDetails pd 
            where pd.id in :detailIds
                        
            """)
    List<PointOfDeliveryView> findPodViewForDetails(List<Long> detailIds);


    @Query("""
            select
                case when count(pd.id) > 0 then true
                else false end
            from PointOfDeliveryDetails pd
            join PointOfDelivery p on p.id = pd.podId
            where p.status in :statuses
            and pd.id = :id
            """)
    boolean existsByIdAndPodStatusIn(Long id, List<PodStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.CacheObjectForPod(pd.id,p.identifier,pd.estimatedMonthlyAvgConsumption)
            from PointOfDelivery p 
            join PointOfDeliveryDetails pd on pd.id=p.lastPodDetailId
            and p.identifier in (:identifiers)
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<CacheObjectForPod> findByPodIdentifiers(List<String> identifiers);

    @Query("""
            select pod from PointOfDeliveryDetails pod
                               join ContractPods cp on cp.podDetailId = pod.id
                               join PointOfDelivery pods on pod.podId = pods.id
                                and pods.status = 'ACTIVE'
                               join ProductContractDetails pcd on cp.contractDetailId = pcd.id
                               join ProductContract pc on pcd.contractId =  pc.id
                               and pc.status = 'ACTIVE'
                              where pod.id = :id
            """)
    List<PointOfDeliveryDetails> checkForBoundObjects(Long id);

    List<PointOfDeliveryDetails> findByPodMeasurementTypeId(Long id);

    @Query("""
            select new bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration.ExcelGenerationLatestPointOfDeliveryDetailsDataModel(
                podd.additionalIdentifier,
                go.gridOperatorCode
            )
            from PointOfDelivery pod
            join PointOfDeliveryDetails podd on pod.id = podd.podId
            join GridOperator go on go.id = pod.gridOperatorId
            where podd.versionId = (
                select max(innerPODD.versionId)
                from PointOfDelivery innerPOD
                join PointOfDeliveryDetails innerPODD on innerPOD.id = innerPODD.podId
                where innerPOD.identifier = :identifier
            )
            and pod.identifier = :identifier
            """)
    Optional<ExcelGenerationLatestPointOfDeliveryDetailsDataModel> findLastVersionPointOfDeliveryDetailsByIdentifier(@Param("identifier") String identifier);
}
