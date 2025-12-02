package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.CustomerAccountManager;
import bg.energo.phoenix.model.enums.customer.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerAccountManagerRepository extends JpaRepository<CustomerAccountManager, Long> {

    @Query("""
        select c
        from CustomerAccountManager c
        where c.status = :status
        and c.customerDetail.id = :customer_details_id
        order by c.createDate
    """)
    List<CustomerAccountManager> getByCustomerDetailsIdAndStatus(
            @Param("customer_details_id") Long customerDetailsId,
            @Param("status") Status active
    );

    Optional<CustomerAccountManager> findByIdAndStatus(Long id,Status status);
}
