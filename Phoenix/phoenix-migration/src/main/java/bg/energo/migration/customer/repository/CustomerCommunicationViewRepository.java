package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.communication.CustomerCommunicationView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerCommunicationViewRepository extends JpaRepository<CustomerCommunicationView, UUID> {
    Optional<CustomerCommunicationView> findByCustomerId(String customerId);

    List<CustomerCommunicationView> findAllByCustomerIdIn(List<String> customerIds);
    CustomerCommunicationView findAllByCustomerId(String customerId);

}
