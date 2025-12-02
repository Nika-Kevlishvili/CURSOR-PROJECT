package bg.energo.phoenix.repository.task;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.task.TaskActivity;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.activity.TaskActivityShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {

    Optional<TaskActivity> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.SystemActivityShortResponse(
                        sa,
                        act,
                        subAct,
                        ta.createDate
                    )
                    from TaskActivity ta
                    join SystemActivity sa on sa.id = ta.systemActivityId
                    join Activity act on sa.activityId = act.id
                    join SubActivity subAct on sa.subActivityId = subAct.id
                        where ta.taskId = :taskId
                        and sa.status in (:statuses)
                        order by ta.createDate asc
                    """
    )
    List<SystemActivityShortResponse> findByTaskIdAndStatusIn(
            @Param("taskId") Long taskId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.activity.TaskActivityShortResponse(
                        t,
                        tt
                    )
                    from TaskActivity ta
                    join Task t on ta.taskId = t.id
                    join TaskType tt on tt.id = t.taskTypeId
                        where ta.systemActivityId = :systemActivityId
                    """
    )
    Optional<TaskActivityShortResponse> getTaskActivityShortResponse(Long systemActivityId);

}