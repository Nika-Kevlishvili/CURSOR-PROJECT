package bg.energo.phoenix.repository.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.action.ActionFile;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentFiles;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAssessmentFilesRepository extends FileExpiration<CustomerAssessmentFiles>, JpaRepository<CustomerAssessmentFiles, Long> {
    Optional<List<CustomerAssessmentFiles>> findAllByCustomerAssessmentIdAndStatus(Long id, EntityStatus status);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT caf.*
             FROM receivable.customer_assessment_files AS caf
                      CROSS JOIN latest_doc_period dp
             WHERE caf.file_url IS NOT NULL
               AND caf.is_archived
               AND caf.status <> 'DELETED'
               AND CURRENT_DATE > ((caf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<CustomerAssessmentFiles> findExpiredFiles();

    @Query("""
            select count(caf.id) > 0
            from CustomerAssessmentFiles caf
            where caf.id <> :currentEntityId
            and caf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM receivable.customer_assessment_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<CustomerAssessmentFiles> findFailedArchivationFiles();

}
