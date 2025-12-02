package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellationNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface InvoiceCancellationNumberRepository extends JpaRepository<InvoiceCancellationNumber, Long> {
    @Query(value = """
            select icn.number
            from invoice.invoice_cancellation_numbers icn
            where EXTRACT(MONTH FROM icn.cancellation_date) = EXTRACT(MONTH FROM cast(:cancellationDate as date))
              and EXTRACT(YEAR FROM icn.cancellation_date) = EXTRACT(YEAR FROM cast(:cancellationDate as date))
            order by icn.number desc
            limit 1
            """, nativeQuery = true)
    Long findLatestCancelledInvoiceNumberByCancellationDate(LocalDate cancellationDate);
}