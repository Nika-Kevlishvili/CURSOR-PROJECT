package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.CustomerPreference;
import bg.energo.phoenix.model.enums.customer.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, Long> {
    List<CustomerPreference> findAllByCustomerDetailId(Long customerDetailId);

    List<CustomerPreference> findAllByCustomerDetailIdAndStatusIn(Long customerDetailId, List<Status> statuses);

    Optional<CustomerPreference> findByPreferencesIdAndCustomerDetailId(Long id, Long customerDetailId);
}
