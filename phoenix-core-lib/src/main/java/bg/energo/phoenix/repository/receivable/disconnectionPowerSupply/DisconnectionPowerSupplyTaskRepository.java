package bg.energo.phoenix.repository.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisconnectionPowerSupplyTaskRepository extends JpaRepository<DisconnectionPowerSupplyTask, Long> {

    Optional<DisconnectionPowerSupplyTask> findByTaskIdAndPowerSupplyDisconnectionIdAndStatus(Long taskId, Long disconnectionId, EntityStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(dops.id, dops.disconnectionNumber, dpst.createDate)
            from DisconnectionPowerSupplyTask dpst
            join DisconnectionOfPowerSupply dops on dops.id = dpst.powerSupplyDisconnectionId
            where dpst.status = 'ACTIVE'
            and dpst.taskId = :taskId
            """)
    List<TaskConnectedEntityResponse> findAllConnectedDisconnections(Long taskId);
}
