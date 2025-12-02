package bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.MLODocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MLODocumentFileRepository extends JpaRepository<MLODocumentFile, Long> {

    @Query(value = """
            select mlofiles.id + 1
            from receivable.mlo_ftp_files mlofiles
            order by mlofiles.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

}
