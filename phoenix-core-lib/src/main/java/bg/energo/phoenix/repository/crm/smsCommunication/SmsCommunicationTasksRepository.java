package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationTasks;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsCommunicationTasksRepository extends JpaRepository<SmsCommunicationTasks,Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(sc.id,sct.createDate) from SmsCommunicationTasks sct
            join SmsCommunication sc on sc.id = sct.smsCommunicationId
            where sct.status = 'ACTIVE'
            and sct.taskId = :taskId
            and sc.communicationChannel=:smsCommunicationChannel
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedSmsCommunication(Long taskId, SmsCommunicationChannel smsCommunicationChannel);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(scc.id,sct.createDate) from SmsCommunicationTasks sct
            join SmsCommunication sc on sc.id = sct.smsCommunicationId
            join SmsCommunicationCustomers scc on scc.smsCommunicationId=sc.id
            where sct.status = 'ACTIVE'
            and sct.taskId = :taskId
            and sc.communicationChannel='SMS'
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedSmsCommunicationSingleSms(Long taskId);

    @Query("""
        select sct from SmsCommunicationTasks sct
        join SmsCommunication sc on sc.id = sct.smsCommunicationId
        where sct.status=:status
        and sc.communicationChannel=:communicationChannel
        and sct.smsCommunicationId=:smsCommunicationId
        and sct.taskId=:taskId
""")
    Optional<SmsCommunicationTasks> findByTaskIdAndSmsCommunicationIdAndStatusAndCommunicationChannel(Long taskId, Long smsCommunicationId, EntityStatus status,SmsCommunicationChannel communicationChannel);

    @Query("""
        select sct from SmsCommunicationTasks sct
        join SmsCommunication sc on sc.id = sct.smsCommunicationId
        join SmsCommunicationCustomers scc on scc.smsCommunicationId=sc.id
        where sct.status=:status
        and sc.communicationChannel='SMS'
        and scc.id=:smsCommunicationId
        and sct.taskId=:taskId
""")
    Optional<SmsCommunicationTasks> findByTaskIdAndSmsCommunicationIdAndStatusAndCommunicationChannelSingleSms(Long taskId, Long smsCommunicationId, EntityStatus status);
}
