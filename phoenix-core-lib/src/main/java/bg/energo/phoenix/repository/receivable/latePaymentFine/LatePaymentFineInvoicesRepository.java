package bg.energo.phoenix.repository.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineInvoices;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LatePaymentFineInvoicesRepository extends JpaRepository<LatePaymentFineInvoices, Long> {
    List<LatePaymentFineInvoices> findAllByLatePaymentFineId(Long latePaymentFineId);

}
