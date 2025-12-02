package bg.energo.phoenix.repository.interestRate;

import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRatePaymentTerms;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterestRatePaymentTermsRepository extends JpaRepository<InterestRatePaymentTerms, Long> {
    Optional<InterestRatePaymentTerms> findByInterestRateIdAndStatus(Long id, InterestRateSubObjectStatus status);
    Optional<InterestRatePaymentTerms> findByIdAndStatus(Long id,InterestRateSubObjectStatus status);
}
