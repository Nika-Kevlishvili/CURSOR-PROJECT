package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.communication.contactpurpose.CustomerCommContactPurposeView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCommContactPurposeViewRepository extends JpaRepository<CustomerCommContactPurposeView, Long> {
}
