package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderExternalIntermediary;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsOrderExternalIntermediaryRepository extends JpaRepository<GoodsOrderExternalIntermediary, Long> {

    List<GoodsOrderExternalIntermediary> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderSubObjectShortResponse(
                        goei.externalIntermediaryId,
                        concat(ei.name, ' (', ei.identifier, ')')
                    )
                    from GoodsOrderExternalIntermediary goei
                    join ExternalIntermediary ei on goei.externalIntermediaryId = ei.id
                        where goei.orderId = :orderId
                        and goei.status in :statuses
                    """
    )
    List<GoodsOrderSubObjectShortResponse> getShortResponseByOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
