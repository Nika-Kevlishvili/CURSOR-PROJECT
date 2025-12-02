package bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationDcnDocFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CancellationDcnDocFileRepository extends JpaRepository<CancellationDcnDocFile, Long> {

    @Query(value = """
            select cancFiles.id + 1
                          from receivable.power_supply_dcn_cancellations_doc_files cancFiles
                          order by cancFiles.id desc
                          limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

}
