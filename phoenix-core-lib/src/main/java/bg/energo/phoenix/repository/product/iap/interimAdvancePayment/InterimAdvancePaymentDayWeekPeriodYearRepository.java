package bg.energo.phoenix.repository.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentDayWeekPeriodYear;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterimAdvancePaymentDayWeekPeriodYearRepository extends JpaRepository<InterimAdvancePaymentDayWeekPeriodYear, Long> {
    List<InterimAdvancePaymentDayWeekPeriodYear> findByInterimAdvancePaymentIdAndStatusIn(Long interimAdvancePaymentId, List<InterimAdvancePaymentSubObjectStatus> statuses);

}
