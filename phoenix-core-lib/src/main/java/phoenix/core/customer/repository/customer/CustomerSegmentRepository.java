package phoenix.core.customer.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import phoenix.core.customer.model.entity.customer.CustomerSegment;

import java.util.List;
import java.util.Optional;

public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, Long> {
    Optional<CustomerSegment> findByIdAndCustomerDetailId(Long id, Long customerDetailId);

    void deleteAllByIdNotInAndCustomerDetailId(List<Long> ids, Long customerDetailId);
}
