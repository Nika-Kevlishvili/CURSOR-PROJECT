package bg.energo.phoenix.repository.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePaymentTerms;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterimAdvancePaymentTermsRepository extends JpaRepository<InterimAdvancePaymentTerms, Long> {
    Optional<InterimAdvancePaymentTerms> findByInterimAdvancePaymentIdAndStatusInOrderByCreateDate(Long interimAdvancePaymentId, List<InterimAdvancePaymentSubObjectStatus> statuses);
    List<InterimAdvancePaymentTerms> findByInterimAdvancePaymentIdAndStatus(Long interimAdvancePaymentId, InterimAdvancePaymentSubObjectStatus status);
    Optional<InterimAdvancePaymentTerms> findByIdAndInterimAdvancePaymentIdAndStatusIn(Long id, Long interimAdvancePaymentId, List<InterimAdvancePaymentSubObjectStatus> statuses);
}
