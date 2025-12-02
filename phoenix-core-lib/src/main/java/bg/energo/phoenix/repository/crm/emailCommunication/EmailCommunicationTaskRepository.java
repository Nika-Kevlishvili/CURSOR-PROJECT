package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailCommunicationTaskRepository extends JpaRepository<EmailCommunicationTask, Long> {
    Optional<EmailCommunicationTask> findByTaskIdAndEmailCommunicationIdAndStatus(Long taskId, Long emailCommunicationId, EntityStatus status);

    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(em.id, em.dmsNumber, ect.createDate) from EmailCommunicationTask ect
            join EmailCommunication em on em.id = ect.emailCommunicationId
            where ect.status = 'ACTIVE'
            and ect.taskId = :taskId
            """
    )
    List<TaskConnectedEntityResponse> findAllConnectedEmailCommunication(Long taskId);

}
