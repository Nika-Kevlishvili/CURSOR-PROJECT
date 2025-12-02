package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunBillingGroup;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRunBillingGroupRepository extends JpaRepository<BillingRunBillingGroup,Long> {

    Optional<BillingRunBillingGroup> findByBillingRunIdAndStatus(Long id, EntityStatus status);

    @Query("""
            select bg
            from BillingRunBillingGroup bg
            where bg.billingRunId = :billingRunId
            and bg.status = :status
            """)
    List<BillingRunBillingGroup> findByBillingRunId(
            @Param("billingRunId") Long billingRunId,
            @Param("status") EntityStatus status
    );

    @Query("""
            select cbg
            from ContractBillingGroup cbg
            join BillingRunBillingGroup brbg on brbg.billingGroupId = cbg.id
            where brbg.billingRunId = :billingRunId
            and brbg.status = 'ACTIVE'
            """)
    Optional<ContractBillingGroup> findBillingGroupByBillingRunId(Long billingRunId);
}
