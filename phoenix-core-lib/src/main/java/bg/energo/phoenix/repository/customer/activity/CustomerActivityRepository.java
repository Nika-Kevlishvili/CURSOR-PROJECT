package bg.energo.phoenix.repository.customer.activity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.activity.CustomerActivity;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerActivityRepository extends JpaRepository<CustomerActivity, Long> {

    Optional<CustomerActivity> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);

    Optional<CustomerActivity> findBySystemActivityIdAndStatusIn(Long systemActivityId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        ca.createDate
                    )
                    from CustomerActivity ca
                    join SystemActivity sa on sa.id = ca.systemActivityId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where ca.customerId = :customerId
                        and sa.status in (:statuses)
                        order by ca.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findByCustomerIdAndStatusIn(
            @Param("customerId") Long customerId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
