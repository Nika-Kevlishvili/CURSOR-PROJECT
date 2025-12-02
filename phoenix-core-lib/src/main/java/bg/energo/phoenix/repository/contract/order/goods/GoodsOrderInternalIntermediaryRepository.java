package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderInternalIntermediary;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsOrderInternalIntermediaryRepository extends JpaRepository<GoodsOrderInternalIntermediary, Long> {

    List<GoodsOrderInternalIntermediary> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderSubObjectShortResponse(
                        goii.accountManagerId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from GoodsOrderInternalIntermediary goii
                    join AccountManager ac on goii.accountManagerId = ac.id
                        where goii.orderId = :orderId
                        and goii.status in :statuses
                    """
    )
    List<GoodsOrderSubObjectShortResponse> getShortResponseByOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
