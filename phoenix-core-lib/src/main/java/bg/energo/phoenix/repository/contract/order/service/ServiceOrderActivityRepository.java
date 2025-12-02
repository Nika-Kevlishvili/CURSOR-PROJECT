package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderActivity;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderActivityRepository extends JpaRepository<ServiceOrderActivity, Long> {

    Optional<ServiceOrderActivity> findBySystemActivityIdAndStatusIn(Long systemActivityId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        soa.createDate
                    )
                    from ServiceOrderActivity soa
                    join SystemActivity sa on sa.id = soa.systemActivityId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where soa.orderId = :serviceOrderId
                        and sa.status in (:statuses)
                        order by soa.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findByOrderIdAndStatusIn(
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
