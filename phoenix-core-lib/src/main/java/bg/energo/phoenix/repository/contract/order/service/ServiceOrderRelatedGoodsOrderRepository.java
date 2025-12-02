package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderRelatedGoodsOrder;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRelatedGoodsOrderRepository extends JpaRepository<ServiceOrderRelatedGoodsOrder, Long> {

    @Query(
            value = """
                    select sorgo from ServiceOrderRelatedGoodsOrder sorgo
                        where (
                            (:type = 'SERVICE_ORDER' and sorgo.serviceOrderId = :objectId)
                            or (:type = 'GOODS_ORDER' and sorgo.goodsOrderId = :objectId)
                        )
                        and sorgo.status in :statuses
                    """
    )
    List<ServiceOrderRelatedGoodsOrder> findByServiceOrderIdOrGoodsOrderIdAndStatusIn(
            @Param("type") String type,
            @Param("objectId") Long objectId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select sorgo from ServiceOrderRelatedGoodsOrder sorgo
                        where sorgo.serviceOrderId = :serviceOrderId
                        and sorgo.goodsOrderId = :goodsOrderId
                        and sorgo.status in :statuses
                    """
    )
    Optional<ServiceOrderRelatedGoodsOrder> findByServiceOrderIdAndGoodsOrderIdAndStatusIn(
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("goodsOrderId") Long goodsOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        sorgo,
                        go.orderNumber,
                        'GOODS_ORDER',
                        go.id
                    )
                    from ServiceOrderRelatedGoodsOrder sorgo
                    join GoodsOrder go on go.id = sorgo.goodsOrderId
                        where sorgo.serviceOrderId = :serviceOrderId
                        and sorgo.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceOrderIdAndStatusIn(
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        sorgo,
                        so.orderNumber,
                        'SERVICE_ORDER',
                        so.id
                    )
                    from ServiceOrderRelatedGoodsOrder sorgo
                    join ServiceOrder so on so.id = sorgo.serviceOrderId
                        where sorgo.goodsOrderId = :goodsOrderId
                        and sorgo.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByGoodsOrderIdAndStatusIn(
            @Param("goodsOrderId") Long goodsOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
