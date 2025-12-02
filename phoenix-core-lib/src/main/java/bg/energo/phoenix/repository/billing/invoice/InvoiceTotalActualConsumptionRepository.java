package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceTotalActualConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceTotalActualConsumptionRepository extends JpaRepository<InvoiceTotalActualConsumption,Long> {
}
