package bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgTasks;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectionToChangeOfCbgTaskRepository extends JpaRepository<ObjectionToChangeOfCbgTasks, Long> {

    @Query("""
            select objTask.taskId
            from ObjectionToChangeOfCbgTasks objTask
            where objTask.changeOfCbgId = :changeOfCbgId
            and objTask.status in :statuses
            """)
    List<Long> findTaskIdsByChangeOfCbgId(Long changeOfCbgId, List<EntityStatus> statuses);

    List<ObjectionToChangeOfCbgTasks> findByChangeOfCbgIdAndTaskIdInAndStatusIn(Long changeOfCbgId, Collection<Long> taskId, Collection<EntityStatus> status);

    List<ObjectionToChangeOfCbgTasks> findByChangeOfCbgIdAndStatusIn(Long changeOfCbgId, Collection<EntityStatus> status);

    List<ObjectionToChangeOfCbgTasks> findByTaskIdNotInAndStatusIn(Collection<Long> taskId, Collection<EntityStatus> status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(o.id,o.changeOfCbgNumber, o.createDate) from ObjectionToChangeOfCbgTasks ot
            join ObjectionToChangeOfCbg o on o.id = ot.changeOfCbgId
            where ot.status = 'ACTIVE'
            and ot.taskId = :taskId
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedToObjectionChangeOfCbg(Long taskId);

    Optional<ObjectionToChangeOfCbgTasks> findByTaskIdAndChangeOfCbgIdAndStatus(Long taskId, Long changeOfCbgId, EntityStatus status);

}
