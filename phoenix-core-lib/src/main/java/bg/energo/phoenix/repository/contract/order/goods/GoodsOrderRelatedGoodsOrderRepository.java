package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderRelatedGoodsOrder;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsOrderRelatedGoodsOrderRepository extends JpaRepository<GoodsOrderRelatedGoodsOrder, Long> {

    @Query(
            value = """
                    select gorgo from GoodsOrderRelatedGoodsOrder gorgo
                        where (gorgo.goodsOrderId = :orderId or gorgo.relatedGoodsOrderId = :orderId)
                        and gorgo.status in :statuses
                    """
    )
    List<GoodsOrderRelatedGoodsOrder> findByOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select gorgo from GoodsOrderRelatedGoodsOrder gorgo
                        where (
                            (gorgo.goodsOrderId = :orderId and gorgo.relatedGoodsOrderId = :relatedOrderId)
                            or (gorgo.goodsOrderId = :relatedOrderId and gorgo.relatedGoodsOrderId = :orderId)
                        )
                        and gorgo.status in :statuses
                    """
    )
    Optional<GoodsOrderRelatedGoodsOrder> findByOrderIdAndRelatedOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("relatedOrderId") Long relatedOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        gorgo,
                        go.orderNumber,
                        'GOODS_ORDER',
                        case when gorgo.goodsOrderId = :orderId
                            then gorgo.relatedGoodsOrderId
                            else gorgo.goodsOrderId
                            end
                    )
                    from GoodsOrderRelatedGoodsOrder gorgo
                    join GoodsOrder go on
                        case when gorgo.goodsOrderId = :orderId
                            then gorgo.relatedGoodsOrderId
                            else gorgo.goodsOrderId
                            end = go.id
                        where (gorgo.goodsOrderId = :orderId or gorgo.relatedGoodsOrderId = :orderId)
                        and gorgo.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByGoodsOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
