package bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsTasks;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisconnectionPowerSupplyRequestsTasksRepository extends JpaRepository<DisconnectionPowerSupplyRequestsTasks, Long> {

    Optional<DisconnectionPowerSupplyRequestsTasks> findByTaskIdAndPowerSupplyDisconnectionRequestIdAndStatus(Long taskId, Long powerSupplyRequestId, EntityStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(dpsr.id, dpsr.requestNumber, dpsrt.createDate) from DisconnectionPowerSupplyRequestsTasks dpsrt
            join DisconnectionPowerSupplyRequests dpsr on dpsr.id = dpsrt.powerSupplyDisconnectionRequestId
            where dpsrt.status = 'ACTIVE'
            and dpsrt.taskId = :taskId
            """)
    List<TaskConnectedEntityResponse> findAllConnectedDisconnectionPowerSupplyRequests(Long taskId);
}
