package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationActivity;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailCommunicationActivityRepository extends JpaRepository<EmailCommunicationActivity, Long> {

    Optional<EmailCommunicationActivity> findByActivityIdAndStatusIn(Long systemActivityId, List<EntityStatus> statuses);

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        emailCommunicationActivity.createDate
                    )
                    from EmailCommunicationActivity emailCommunicationActivity
                    join SystemActivity sa on sa.id = emailCommunicationActivity.activityId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where emailCommunicationActivity.emailCommunicationId = :emailCommunicationId
                        and sa.status in (:statuses)
                        order by emailCommunicationActivity.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findByEmailCommunicationIdAndStatus(
            @Param("emailCommunicationId") Long emailCommunicationId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
