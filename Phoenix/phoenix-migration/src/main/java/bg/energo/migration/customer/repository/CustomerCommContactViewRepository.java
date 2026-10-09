package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.communication.contacts.CustomerCommContactView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerCommContactViewRepository extends JpaRepository<CustomerCommContactView, Long> {
    List<CustomerCommContactView> findAllByCustomerId(String customerId);
    List<CustomerCommContactView> findAllByCustomerIdentifier(String customerIdentifier);
    List<CustomerCommContactView> findAllByCustomerIdIn(List<String> customerIds);
}
