package bg.energo.phoenix.process.repository;

import bg.energo.phoenix.model.request.contract.pod.ProcessActionFilterModel;
import bg.energo.phoenix.process.model.entity.ProcessContractPods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessContractPodRepository extends JpaRepository<ProcessContractPods, Long> {

    @Query("""
            select new bg.energo.phoenix.model.request.contract.pod.ProcessActionFilterModel(pcd.id,pcd.contractId,pcd.startDate,cp,cd.id,podd.podId,cd.customerId,pcp.recordInfoId)
            from ProcessContractPods pcp 
            join ProcessedRecordInfo  pri on pri.id=pcp.recordInfoId
            join ContractPods cp on cp.id = pcp.contractPodId
            join ProductContractDetails pcd on pcd.id= cp.contractDetailId
            join CustomerDetails cd on cd.id=pcd.customerDetailId
            join PointOfDeliveryDetails  podd on podd.id=cp.podDetailId
            where pri.processId=:processId
            """)
    List<ProcessActionFilterModel> findActionModelsByProcessId(Long processId);

    @Query("""
            delete from ProcessContractPods pcp
            where pcp.recordInfoId in (
            select pri.id from ProcessedRecordInfo pri
            where pri.processId=:id
            )
            """)
    @Modifying
    void deleteAllByProcessId(Long id);
}
