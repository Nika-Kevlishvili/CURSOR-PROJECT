package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.DeductionInterimInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeductionInterimInvoiceRepository extends JpaRepository<DeductionInterimInvoice, Long> {


}
