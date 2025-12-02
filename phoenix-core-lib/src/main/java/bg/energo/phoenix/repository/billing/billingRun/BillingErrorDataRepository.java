package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingErrorData;
import bg.energo.phoenix.model.response.billing.billingRun.BillingErrorDataResponse;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingErrorDataRepository extends JpaRepository<BillingErrorData, Long> {
    @Query("""
            select new bg.energo.phoenix.model.response.billing.billingRun.BillingErrorDataResponse(bed.invoiceNumbers,bed.errorMessage,bed.billingRunId)
            from BillingErrorData  bed
            where bed.billingRunId=:billingId
            and bed.errorProtocol = :protocol
            """)
    List<BillingErrorDataResponse> findByBillingId(Long billingId, BillingProtocol protocol);

    boolean existsByBillingRunId(Long billingRunId);
}