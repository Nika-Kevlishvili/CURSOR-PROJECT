package phoenix.core.customer.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import phoenix.core.customer.model.entity.customer.ConnectedGroup;

public interface ConnectedGroupRepository extends JpaRepository<ConnectedGroup, Long> {
}
