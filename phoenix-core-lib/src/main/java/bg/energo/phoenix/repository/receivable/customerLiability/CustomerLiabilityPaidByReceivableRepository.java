package bg.energo.phoenix.repository.receivable.customerLiability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerLiabilityPaidByReceivableRepository extends JpaRepository<CustomerLiabilityPaidByReceivable, Long> {
    List<CustomerLiabilityPaidByReceivable> findByCustomerLiabilityIdAndStatus(Long customerLiabilityId, EntityStatus status);

    boolean existsByCustomerReceivableIdAndStatus(Long customerReceivableId, EntityStatus status);


    @Query("""
        select  clpr from CustomerLiabilityPaidByReceivable clpr
        where clpr.operationContext='ALO' and clpr.customerLiabilityId in (:liabilitieIds)
""")
    List<CustomerLiabilityPaidByReceivable> findByLiabilitieIds(List<Long> liabilitieIds);
}
