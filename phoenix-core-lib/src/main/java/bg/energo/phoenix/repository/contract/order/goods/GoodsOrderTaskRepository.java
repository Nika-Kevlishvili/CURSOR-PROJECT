package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsOrderTaskRepository extends JpaRepository<GoodsOrderTask, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(go.id, go.orderNumber, got.createDate) from GoodsOrderTask got
            join GoodsOrder go on go.id = got.orderId
            where got.status = 'ACTIVE'
            and got.taskId = :taskId
            and go.status = 'ACTIVE'
            """)
    List<TaskConnectedEntityResponse> findAllConnectedOrders(Long taskId);

    @Query("""
            select got from GoodsOrderTask got
            where got.taskId = :taskId
            and got.orderId = :orderId
            and got.status = 'ACTIVE'
            """)
    Optional<GoodsOrderTask> findGoodsOrderTaskByTaskIdAndOrderId(Long taskId, Long orderId);
}
