package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunDraftInvoicesMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BillingRunDraftInvoicesMarkRepository extends JpaRepository<BillingRunDraftInvoicesMark, Long> {
    @Modifying
    @Transactional
    @Query("""
            delete from BillingRunDraftInvoicesMark mark
            where mark.billingRun = :billingRun
            and ((:invoices) is null or  mark.invoice in (:invoices))
            """)
    void deleteAllByBillingRunAndInvoiceIn(Long billingRun,
                                           List<Long> invoices);

    @Modifying
    @Transactional
    @Query("""
            delete from BillingRunDraftInvoicesMark mark
            where mark.billingRun = :billingRun
            and (coalesce(:invoices, '') = '' or mark.invoice not in (:invoices))
            """)
    void deleteAllByBillingRunAndInvoiceNotIn(Long billingRun,
                                              List<Long> invoices);

    @Query("""
            select mark.invoice
            from BillingRunDraftInvoicesMark mark
            where mark.billingRun = :billingRun
            """)
    List<Long> findMarkedInvoicesByBillingRun(Long billingRun);
}