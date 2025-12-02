package bg.energo.phoenix.repository.interestRate;

import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRatePeriods;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InterestRatePeriodsRepository extends JpaRepository<InterestRatePeriods, Long> {

    List<InterestRatePeriods> findByInterestRateIdAndStatus(Long id, InterestRatePeriodStatus status);

    @Query("""
            select irp from InterestRatePeriods irp where irp.id not in (:periodIds) and irp.interestRateId = :id
            """)
    List<InterestRatePeriods> findByIdNotInAndInterestRateIds(Long id, List<Long> periodIds);

    List<InterestRatePeriods> findByIdNotInAndInterestRateId(List<Long> periodIds,Long id);

    Optional<InterestRatePeriods> findByIdAndStatusAndInterestRateId(Long id, InterestRatePeriodStatus status,Long interestRateId);

}
