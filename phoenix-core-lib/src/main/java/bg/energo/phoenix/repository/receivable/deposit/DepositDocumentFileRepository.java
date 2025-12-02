package bg.energo.phoenix.repository.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositDocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface DepositDocumentFileRepository extends JpaRepository<DepositDocumentFile, Long> {

    @Query(value = """
            select depositFiles.id + 1
            from receivable.customer_deposit_ftp_files depositFiles
            order by depositFiles.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

    @Query("""
                    select df from DepositDocumentFile df
                    where df.depositId in :depositIds
                    and df.status = :status
            """)
    Set<DepositDocumentFile> findByReschedulingIdAndStatus(List<Long> depositIds, EntityStatus status);


}
