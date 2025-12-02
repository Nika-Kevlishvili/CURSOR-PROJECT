package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedGoodsOrder;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceContractRelatedGoodsOrderRepository extends JpaRepository<ServiceContractRelatedGoodsOrder, Long> {

    @Query(
            value = """
                    select scrgo from ServiceContractRelatedGoodsOrder scrgo
                        where (
                            (:type = 'SERVICE_CONTRACT' and scrgo.serviceContractId = :objectId)
                            or (:type = 'GOODS_ORDER' and scrgo.goodsOrderId = :objectId)
                        )
                        and scrgo.status in :statuses
                    """
    )
    List<ServiceContractRelatedGoodsOrder> findByServiceContractIdOrGoodsOrderIdAndStatusIn(
            @Param("type") String type,
            @Param("objectId") Long objectId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select scrgo from ServiceContractRelatedGoodsOrder scrgo
                        where scrgo.serviceContractId = :serviceContractId
                        and scrgo.goodsOrderId = :goodsOrderId
                        and scrgo.status in :statuses
                    """
    )
    Optional<ServiceContractRelatedGoodsOrder> findByServiceContractIdAndGoodsOrderIdAndStatusIn(
            @Param("serviceContractId") Long serviceContractId,
            @Param("goodsOrderId") Long goodsOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        scrgo,
                        go.orderNumber,
                        'GOODS_ORDER',
                        go.id
                    )
                    from ServiceContractRelatedGoodsOrder scrgo
                    join GoodsOrder go on go.id = scrgo.goodsOrderId
                        where scrgo.serviceContractId = :serviceContractId
                        and scrgo.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceContractIdAndStatusIn(
            @Param("serviceContractId") Long serviceContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        scrgo,
                        sc.contractNumber,
                        'SERVICE_CONTRACT',
                        sc.id
                    )
                    from ServiceContractRelatedGoodsOrder scrgo
                    join ServiceContracts sc on sc.id = scrgo.serviceContractId
                        where scrgo.goodsOrderId = :goodsOrderId
                        and scrgo.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByGoodsOrderIdAndStatusIn(
            @Param("goodsOrderId") Long goodsOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
