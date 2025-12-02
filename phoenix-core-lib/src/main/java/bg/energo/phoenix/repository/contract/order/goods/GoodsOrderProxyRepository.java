package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxies;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoodsOrderProxyRepository extends JpaRepository<GoodsOrderProxies,Long> {
    List<GoodsOrderProxies> findByOrderIdAndStatusIn(Long id, List<EntityStatus> statuses);
    List<GoodsOrderProxies> findByIdNotInAndStatusInAndOrderId(List<Long> ids, List<EntityStatus> statuses,Long orderId);

    Optional<GoodsOrderProxies> findByIdAndStatus(Long id, EntityStatus status);
}
