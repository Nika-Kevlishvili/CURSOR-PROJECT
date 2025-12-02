package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.CustomerSegment;
import bg.energo.phoenix.model.enums.customer.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, Long> {
    Optional<CustomerSegment> findBySegmentIdAndCustomerDetailId(Long id, Long customerDetailId);

    List<CustomerSegment> findAllByCustomerDetailId(Long id);
    List<CustomerSegment> findAllByCustomerDetailIdAndStatus(Long id, Status status);

}
