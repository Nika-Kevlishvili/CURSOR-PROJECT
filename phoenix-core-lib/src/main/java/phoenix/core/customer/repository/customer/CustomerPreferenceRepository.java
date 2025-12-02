package phoenix.core.customer.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.CustomerPreference;

import java.util.List;
import java.util.Optional;

public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, Long> {
    Optional<CustomerPreference> findByIdAndCustomerDetailId(@Param("id") Long id, @Param("customerDetailId") Long customerDetailId);

    void deleteAllByIdNotInAndCustomerDetailId(List<Long> ids, Long customerDetailId);
}
