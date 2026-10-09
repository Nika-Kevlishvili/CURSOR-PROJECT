package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.manager.CustomerManagerView;
import bg.energo.migration.customer.models.CustomerManagerViewMiddleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerManagerViewRepository extends JpaRepository<CustomerManagerView, Long> {
    List<CustomerManagerView> findAllByCustomerId(String customerId);

    @Query(nativeQuery = true, value = """
        select distinct manager.personal_number,
                        manager.customer_id,
                        manager.name,
                        manager.middle_name,
                        manager.surname,
                        manager.title_id,
                        manager.job_position,
                        manager.representation_method_id,
                        manager.status
        from customer_migration.v_customer_managers manager
        left join customer_migration.customers cust on cust.customer_id = manager.customer_id
        where cust.customer_id = :customerId
    """)
    List<CustomerManagerViewMiddleResponse> findDistinctByPersonalNumber(@Param("customerId") String customerId);

    @Query(nativeQuery = true, value = """
        select distinct manager.personal_number,
                        manager.customer_id,
                        manager.name,
                        manager.middle_name,
                        manager.surname,
                        manager.title_id,
                        manager.job_position,
                        manager.representation_method_id,
                        manager.status
        from customer_migration.v_customer_managers manager
        left join customer_migration.customers cust on cust.customer_id = manager.customer_id
        where cust.customer_id in :customerIds
    """)
    List<CustomerManagerViewMiddleResponse> findDistinctByPersonalNumberIn(@Param("customerIds") List<String> customerIds);

}
