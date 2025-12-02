package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderTaskRepository extends JpaRepository<ServiceOrderTask, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(so.id, so.orderNumber, sot.createDate) from ServiceOrderTask sot
            join ServiceOrder so on so.id = sot.orderId
            where sot.status = 'ACTIVE'
            and sot.taskId = :taskId
            and so.status = 'ACTIVE'
            """)
    List<TaskConnectedEntityResponse> findAllConnectedOrders(Long taskId);

    @Query("""
            select sot from ServiceOrderTask sot
            where sot.taskId = :taskId
            and sot.orderId = :orderId
            and sot.status = 'ACTIVE'
            """)
    Optional<ServiceOrderTask> findServiceOrderTaskByTaskIdAndOrderId(Long taskId, Long orderId);
}
