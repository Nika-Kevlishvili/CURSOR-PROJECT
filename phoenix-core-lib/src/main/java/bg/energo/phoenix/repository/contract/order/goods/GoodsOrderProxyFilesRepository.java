package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxyFiles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoodsOrderProxyFilesRepository extends JpaRepository<GoodsOrderProxyFiles, Long> {
    Optional<GoodsOrderProxyFiles> findByIdAndStatus(Long id, EntityStatus status);

    List<GoodsOrderProxyFiles> findByOrderProxyIdAndStatusIn(Long id, List<EntityStatus> active);
}
