package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingNotification;
import bg.energo.phoenix.model.response.billing.billingRun.BillingNotificationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingNotificationRepository extends JpaRepository<BillingNotification, Long> {
    List<BillingNotification> findAllByBilling(Long billingRunId);

    @Query("""
            select new bg.energo.phoenix.model.response.billing.billingRun.BillingNotificationResponse(am,pt,bn.type)
            from BillingNotification bn 
            left join AccountManager am on am.id=bn.employee
            left join PortalTag pt on pt.id=bn.tag
            where bn.billing=:billingRunId
            """)
    List<BillingNotificationResponse> findBillingResponse(Long billingRunId);
}
