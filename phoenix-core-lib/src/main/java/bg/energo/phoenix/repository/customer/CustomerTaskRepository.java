package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.CustomerTask;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerTaskRepository extends JpaRepository<CustomerTask, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse(
                c.id,
                case when c.customerType = 'LEGAL_ENTITY' then concat(coalesce(cd.name, '') , coalesce(lf.name, ''))
                    else concat(coalesce(cd.name, '') , ' ', coalesce(cd.middleName, '') , ' ', coalesce(cd.lastName, '')) end,
                ct.createDate,
                c.identifier
            )
            from CustomerTask ct
            join Customer c on c.id = ct.customerId
            join CustomerDetails cd on c.lastCustomerDetailId = cd.id
            left join LegalForm lf on lf.id = cd.legalFormId
            where ct.taskId = :taskId
            and ct.status = 'ACTIVE'
            and c.status = 'ACTIVE'
            """)
    List<TaskConnectedEntityResponse> findAllConnectedCustomersMapToResponse(@Param("taskId") Long taskId);

    @Query("""
            select ct.customerId from CustomerTask ct
                        join Customer c on c.id = ct.customerId
            join CustomerDetails cd on c.lastCustomerDetailId = cd.id
            where ct.taskId = :taskId
            and ct.status = 'ACTIVE'
            and c.status = 'ACTIVE'
            """)
    List<Long> findAllConnectedCustomerIds(@Param("taskId") Long taskId);

    @Query("""
            select ct from CustomerTask ct
            where ct.taskId = :taskId
            and ct.customerId = :customerId
            and ct.status = 'ACTIVE'
            """)
    Optional<CustomerTask> findCustomerTaskByTaskIdAndCustomerId(Long taskId, Long customerId);
}