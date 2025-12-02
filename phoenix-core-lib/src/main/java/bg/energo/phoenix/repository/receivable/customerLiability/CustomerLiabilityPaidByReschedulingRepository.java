package bg.energo.phoenix.repository.receivable.customerLiability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByRescheduling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerLiabilityPaidByReschedulingRepository extends JpaRepository<CustomerLiabilityPaidByRescheduling, Long> {
    List<CustomerLiabilityPaidByRescheduling> findByCustomerLiabilityIdAndStatus(Long customerLiabilityId, EntityStatus status);

    @Query("""
           select clr.id from CustomerLiabilityPaidByRescheduling clr
                      where clr.customerReschedulingId=:reschedulingId
            """)
    List<Long> findByReschedulingId(Long reschedulingId);
}
