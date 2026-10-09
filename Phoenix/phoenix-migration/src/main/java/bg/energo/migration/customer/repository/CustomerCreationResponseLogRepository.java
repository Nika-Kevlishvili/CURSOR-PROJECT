package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.migrationlog.CustomerCreationResponseLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCreationResponseLogRepository extends JpaRepository<CustomerCreationResponseLog, Long> {
}
