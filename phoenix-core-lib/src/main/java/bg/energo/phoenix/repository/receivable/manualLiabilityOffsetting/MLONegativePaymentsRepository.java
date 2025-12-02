package bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLONegativePayments;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MLONegativePaymentsRepository extends JpaRepository<MLONegativePayments, Long> {

    @Query("""
            select new bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.LiabilitiesOffsettingChoice(
            p.id,
            concat(p.paymentNumber, '/', p.paymentDate, ' | ', p.paymentDate, ' | ', p.currentAmount, ' ', c.name),
            p.customerId,
            p.currentAmount,
            c.id
            )
            from MLONegativePayments mlonp
            join Payment p on mlonp.customerPaymentId = p.id
            join Currency c on p.currencyId  = c.id
            where mlonp.manualLiabilityOffsettingId = :mloId
            """)
    List<LiabilitiesOffsettingChoice> getNegativePaymentsByMLOId(Long mloId);
}
