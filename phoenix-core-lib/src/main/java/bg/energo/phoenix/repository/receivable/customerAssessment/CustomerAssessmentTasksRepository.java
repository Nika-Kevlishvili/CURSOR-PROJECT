package bg.energo.phoenix.repository.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentTasks;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAssessmentTasksRepository extends JpaRepository<CustomerAssessmentTasks, Long> {

    List<CustomerAssessmentTasks> findByCustomerAssessmentIdAndTaskIdInAndStatusIn(Long customerAssessmentId, Collection<Long> taskId, Collection<EntityStatus> status);

    List<CustomerAssessmentTasks> findByCustomerAssessmentIdAndStatusIn(Long customerAssessmentId, Collection<EntityStatus> status);

    List<CustomerAssessmentTasks> findByTaskIdNotInAndStatusIn(Collection<Long> taskId, Collection<EntityStatus> status);

    @Query("""
        select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(
            ca.id,
            case when c.customerType = 'LEGAL_ENTITY' then cd.name
                else concat(coalesce(cd.name, '') , ' ', coalesce(cd.middleName, '') , ' ', coalesce(cd.lastName, '')) end,
            cat.createDate
        )
        from CustomerAssessmentTasks cat
        join CustomerAssessment ca on ca.id = cat.customerAssessmentId
        join Customer c on c.id = ca.customerId
        join CustomerDetails cd on c.lastCustomerDetailId = cd.id
        where cat.taskId = :taskId
        and cat.status = 'ACTIVE'
        and ca.status = 'ACTIVE'
        and c.status = 'ACTIVE'
        """)
    List<TaskConnectedEntityResponse> findAllConnectedCustomerAssessmentsMapToResponse(@Param("taskId") Long taskId);

    @Query("""
            select cat from CustomerAssessmentTasks cat
            where cat.taskId = :taskId
            and cat.customerAssessmentId = :customerAssessmentId
            and cat.status = 'ACTIVE'
            """)
    Optional<CustomerAssessmentTasks> findCustomerTaskByTaskIdAndCustomerAssessmentId(Long taskId, Long customerAssessmentId);

}
