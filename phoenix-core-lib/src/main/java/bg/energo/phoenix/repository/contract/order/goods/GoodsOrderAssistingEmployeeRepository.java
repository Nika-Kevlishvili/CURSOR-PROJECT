package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderAssistingEmployee;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsOrderAssistingEmployeeRepository extends JpaRepository<GoodsOrderAssistingEmployee, Long> {

    List<GoodsOrderAssistingEmployee> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderSubObjectShortResponse(
                        goae.accountManagerId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from GoodsOrderAssistingEmployee goae
                    join AccountManager ac on goae.accountManagerId = ac.id
                        where goae.orderId = :orderId
                        and goae.status in :statuses
                    """
    )
    List<GoodsOrderSubObjectShortResponse> getShortResponseByOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
