package bg.energo.phoenix.repository.contract.activity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.activity.ServiceContractActivity;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceContractActivityRepository extends JpaRepository<ServiceContractActivity, Long> {

    Optional<ServiceContractActivity> findBySystemActivityIdAndStatusIn(Long systemActivityId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        sca.createDate
                    )
                    from ServiceContractActivity sca
                    join SystemActivity sa on sa.id = sca.systemActivityId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where sca.contractId = :serviceContractId
                        and sa.status in (:statuses)
                        order by sca.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findByContractIdAndStatusIn(
            @Param("serviceContractId") Long serviceContractId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
