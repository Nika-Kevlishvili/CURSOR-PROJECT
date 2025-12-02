package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderInternalIntermediary;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderInternalIntermediaryRepository extends JpaRepository<ServiceOrderInternalIntermediary, Long> {

    List<ServiceOrderInternalIntermediary> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse(
                        soii.accountManagerId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ServiceOrderInternalIntermediary soii
                    join AccountManager ac on soii.accountManagerId = ac.id
                        where soii.orderId = :orderId
                        and soii.status in :statuses
                        order by soii.createDate asc
                    """
    )
    List<ServiceOrderSubObjectShortResponse> getShortResponseByOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
