package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationActivity;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsCommunicationActivityRepository extends JpaRepository<SmsCommunicationActivity,Long> {
    @Query(
        value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        smsCommunicationActivity.createDate
                    )
                    from SmsCommunicationActivity smsCommunicationActivity
                    join SystemActivity sa on sa.id = smsCommunicationActivity.activityId
                    join SmsCommunication sc on sc.id = smsCommunicationActivity.smsCommunicationId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where smsCommunicationActivity.smsCommunicationId = :smsCommunicationId
                        and sa.status in (:statuses)
                        and sc.communicationChannel=:communicationChannel
                        order by smsCommunicationActivity.createDate asc
                    """
)
List<SystemActivityShortResponse> findBySmsCommunicationIdAndStatus(
                @Param("smsCommunicationId") Long smsCommunicationId,
                @Param("statuses") List<EntityStatus> statuses,
                SmsCommunicationChannel communicationChannel
        );

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        smsCommunicationActivity.createDate
                    )
                    from SmsCommunicationActivity smsCommunicationActivity
                    join SystemActivity sa on sa.id = smsCommunicationActivity.activityId
                    join SmsCommunication sc on sc.id = smsCommunicationActivity.smsCommunicationId
                    join SmsCommunicationCustomers scc on scc.smsCommunicationId=sc.id
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where scc.id=:smsCommunicationId
                        and sa.status in (:statuses)
                        and sc.communicationChannel=:communicationChannel
                        order by smsCommunicationActivity.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findBySmsCommunicationIdAndStatusSingleSMS(
            @Param("smsCommunicationId") Long smsCommunicationId,
            @Param("statuses") List<EntityStatus> statuses,
            SmsCommunicationChannel communicationChannel
    );

    Optional<SmsCommunicationActivity> findByActivityIdAndStatusIn(Long systemActivityId, List<EntityStatus> statuses);


}
