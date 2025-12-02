package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxyManagers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsOrderProxyManagersRepository extends JpaRepository<GoodsOrderProxyManagers, Long> {

    List<GoodsOrderProxyManagers> findByOrderProxyIdAndStatus(Long id, EntityStatus contractSubObjectStatus);
}
