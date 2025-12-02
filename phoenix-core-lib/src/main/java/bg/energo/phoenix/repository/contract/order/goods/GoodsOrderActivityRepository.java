package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderActivity;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsOrderActivityRepository extends JpaRepository<GoodsOrderActivity, Long> {

    Optional<GoodsOrderActivity> findBySystemActivityIdAndStatusIn(Long systemActivityId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        goa.createDate
                    )
                    from GoodsOrderActivity goa
                    join SystemActivity sa on sa.id = goa.systemActivityId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where goa.orderId = :goodsOrderId
                        and sa.status in (:statuses)
                        order by goa.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findByOrderIdAndStatusIn(
            @Param("goodsOrderId") Long goodsOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
