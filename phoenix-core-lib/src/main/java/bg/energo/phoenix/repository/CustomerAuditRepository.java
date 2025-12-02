package bg.energo.phoenix.repository;

import bg.energo.phoenix.model.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAuditRepository extends JpaRepository<Customer, Long> {
}
