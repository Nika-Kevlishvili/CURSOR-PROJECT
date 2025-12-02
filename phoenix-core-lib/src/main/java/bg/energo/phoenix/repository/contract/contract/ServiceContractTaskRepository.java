package bg.energo.phoenix.repository.contract.contract;

import bg.energo.phoenix.model.entity.contract.contract.ServiceContractTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceContractTaskRepository extends JpaRepository<ServiceContractTask, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(sc.id, sc.contractNumber, sct.createDate) from ServiceContractTask sct
            join ServiceContracts sc on sc.id = sct.contractId
            where sct.taskId = :taskId
            and sct.status = 'ACTIVE'
            and sc.status = 'ACTIVE'
            """)
    List<TaskConnectedEntityResponse> findAllConnectedContracts(Long taskId);

    @Query("""
            select sct.id from ServiceContractTask sct
            where sct.taskId = :taskId
            """)
    List<Long> findAllContractIdByTaskId(Long taskId);

    @Query("""
            select sct from ServiceContractTask sct
            where sct.taskId = :taskId
            and sct.contractId = :contractId
            and sct.status = 'ACTIVE'
            """)
    Optional<ServiceContractTask> findServiceContractTaskByTaskIdAndContractId(Long taskId, Long contractId);
}
