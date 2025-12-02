package bg.energo.phoenix.service.billing.invoice.numberGeneration;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceNumberTableRepository extends JpaRepository<InvoiceNumberTable,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoiceNumberTable> findFirstByNumberType(NumberType invoiceType);
}
