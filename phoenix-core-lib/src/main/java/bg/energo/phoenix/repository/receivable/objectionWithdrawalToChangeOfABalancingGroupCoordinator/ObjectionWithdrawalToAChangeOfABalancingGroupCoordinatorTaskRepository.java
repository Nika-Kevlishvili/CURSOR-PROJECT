package bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository extends JpaRepository<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask, Long> {

    Optional<List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask>> findAllByWithdrawalChangeOfCbgId(Long withdrawalId);

    List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask> findByWithdrawalChangeOfCbgIdAndTaskIdInAndStatusIn(Long withdrawalChangeOfCbgId, Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);

    List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask> findByWithdrawalChangeOfCbgIdAndStatusIn(Long withdrawalChangeOfCbgId, Collection<ReceivableSubObjectStatus> status);

    List<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask> findByTaskIdNotInAndStatusIn(Collection<Long> taskId, Collection<ReceivableSubObjectStatus> status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(o.id,o.withdrawalChangeOfCbgNumber, o.createDate) from ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask ot
            join ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator o on o.id = ot.withdrawalChangeOfCbgId
            where ot.status = 'ACTIVE'
            and ot.taskId = :taskId
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedToObjectionWithdrawalToAChangeOfCbg(Long taskId);

    Optional<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask> findByTaskIdAndWithdrawalChangeOfCbgIdAndStatus(Long taskId, Long withdrawalChangeOfCbgId, ReceivableSubObjectStatus status);

}
