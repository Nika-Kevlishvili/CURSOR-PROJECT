package bg.energo.phoenix.repository.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentDateOfMonth;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface InterimAdvancePaymentDateOfMonthRepository extends JpaRepository<InterimAdvancePaymentDateOfMonth, Long> {
    List<InterimAdvancePaymentDateOfMonth> findByInterimAdvancePaymentIdAndStatusIn(Long interimAdvancePaymentId, List<InterimAdvancePaymentSubObjectStatus> statuses);

}
