package phoenix.core.customer.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import phoenix.core.customer.model.entity.customer.CustomerAccountManager;

public interface CustomerAccountManagerRepository extends JpaRepository<CustomerAccountManager, Long> {
}
