package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderPaymentTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsOrderPaymentTermRepository extends JpaRepository<GoodsOrderPaymentTerm, Long> {
    List<GoodsOrderPaymentTerm> findAllByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);

    @Query("""
            select gopt
            from GoodsOrderPaymentTerm gopt
            where gopt.orderId = :orderId
            and gopt.status = 'ACTIVE'
            """)
    Optional<GoodsOrderPaymentTerm> findActiveGoodsOrderPaymentTerm(Long orderId);
}