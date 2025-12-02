package bg.energo.phoenix.repository.receivable.payment;

import bg.energo.phoenix.model.entity.receivable.payment.PaymentLiabilityOffsetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentLiabilityOffsettingRepository extends JpaRepository<PaymentLiabilityOffsetting, Long> {

}
