package bg.energo.phoenix.repository.contract.contract;

import bg.energo.phoenix.model.entity.contract.contract.ProductContractTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractTaskRepository extends JpaRepository<ProductContractTask, Long> {
    @Query("""
            select ct.contractId from ProductContractTask ct
            where ct.taskId = :id
            """)
    List<Long> findAllContractIdByTaskId(@Param("id") Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(pc.id, pc.contractNumber, pct.createDate) from ProductContractTask pct
            join ProductContract pc on pc.id = pct.contractId
            where pct.taskId = :taskId
            and pct.status = 'ACTIVE'
            and pc.status = 'ACTIVE'
            """)
    List<TaskConnectedEntityResponse> findAllConnectedContracts(@Param("taskId") Long taskId);

    @Query("""
            select pct from ProductContractTask pct
            where pct.taskId = :taskId
            and pct.contractId = :productContractId
            and pct.status = 'ACTIVE'
            """)
    Optional<ProductContractTask> findProductContractTaskByTaskIdAndContractId(Long taskId, Long productContractId);
}