package bg.energo.phoenix.repository.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineDocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LatePaymentFineDocumentFileRepository extends JpaRepository<LatePaymentFineDocumentFile, Long> {

    boolean existsLatePaymentFineDocumentFileByLatePaymentId(Long latePaymentFineId);

    @Query(value = """
            select lpfff.id + 1
            from receivable.late_payment_fine_ftp_files lpfff
            order by lpfff.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

}
