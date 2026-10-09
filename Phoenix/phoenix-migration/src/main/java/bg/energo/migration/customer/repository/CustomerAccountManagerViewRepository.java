package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.accountmanager.CustomerAccountManagerView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAccountManagerViewRepository extends JpaRepository<CustomerAccountManagerView, Long> {
    List<CustomerAccountManagerView> findAllByCustomerId(String customerId);
    List<CustomerAccountManagerView> findAllByCustomerIdIn(List<String> customerIds);
}
