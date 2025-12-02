package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.billing.BillingInvoicesFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingInvoicesFileRepository extends JpaRepository<BillingInvoicesFile, Long> {

    Optional<BillingInvoicesFile> findByIdAndStatusIn(Long id, List<EntityStatus> status);

    Optional<BillingInvoicesFile> findByBillingIdAndStatus(Long id, EntityStatus status);

    @Modifying
    @Query("""
            update BillingInvoicesFile bif
            set bif.status = 'DELETED'
            where bif.billingId = :billingId
            """)
    void updateFileStatusByBillingId(@Param("billingId") Long billingId);

}
