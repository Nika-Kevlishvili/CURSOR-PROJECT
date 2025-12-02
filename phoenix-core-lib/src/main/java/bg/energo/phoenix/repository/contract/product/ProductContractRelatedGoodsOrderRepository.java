package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedGoodsOrder;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractRelatedGoodsOrderRepository extends JpaRepository<ProductContractRelatedGoodsOrder, Long> {

    @Query(
            value = """
                    select pcrgo from ProductContractRelatedGoodsOrder pcrgo
                        where (
                            (:type = 'PRODUCT_CONTRACT' and pcrgo.productContractId = :objectId)
                            or (:type = 'GOODS_ORDER' and pcrgo.goodsOrderId = :objectId)
                        )
                        and pcrgo.status in :statuses
                    """
    )
    List<ProductContractRelatedGoodsOrder> findByProductContractIdOrGoodsOrderIdAndStatusIn(
            @Param("type") String type,
            @Param("objectId") Long objectId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select pcrgo from ProductContractRelatedGoodsOrder pcrgo
                        where pcrgo.productContractId = :productContractId
                        and pcrgo.goodsOrderId = :goodsOrderId
                        and pcrgo.status in :statuses
                    """
    )
    Optional<ProductContractRelatedGoodsOrder> findByProductContractIdAndGoodsOrderIdAndStatusIn(
            @Param("productContractId") Long productContractId,
            @Param("goodsOrderId") Long goodsOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        pcrgo,
                        go.orderNumber,
                        'GOODS_ORDER',
                        go.id
                    )
                    from ProductContractRelatedGoodsOrder pcrgo
                    join GoodsOrder go on go.id = pcrgo.goodsOrderId
                        where pcrgo.productContractId = :productContractId
                        and pcrgo.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByProductContractIdAndStatusIn(
            @Param("productContractId") Long productContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        pcrgo,
                        pc.contractNumber,
                        'PRODUCT_CONTRACT',
                        pc.id
                    )
                    from ProductContractRelatedGoodsOrder pcrgo
                    join ProductContract pc on pc.id = pcrgo.productContractId
                        where pcrgo.goodsOrderId = :goodsOrderId
                        and pcrgo.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByGoodsOrderIdAndStatusIn(
            @Param("goodsOrderId") Long goodsOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
