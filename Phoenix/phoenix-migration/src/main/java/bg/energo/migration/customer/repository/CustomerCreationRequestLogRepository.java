package bg.energo.migration.customer.repository;

import bg.energo.migration.customer.entity.migrationlog.CustomerCreationRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCreationRequestLogRepository extends JpaRepository<CustomerCreationRequestLog, Long> {
}
