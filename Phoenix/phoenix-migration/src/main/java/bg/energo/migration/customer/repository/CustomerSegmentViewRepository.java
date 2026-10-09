package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.segment.CustomerSegmentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerSegmentViewRepository extends JpaRepository<CustomerSegmentView, Long> {
    List<CustomerSegmentView> findAllByCustomerId(String customerId);

    List<CustomerSegmentView> findAllByCustomerIdIn(List<String> customerIds);

}
