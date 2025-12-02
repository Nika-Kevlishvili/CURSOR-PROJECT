package bg.energo.phoenix.repository.crm.emailCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationFile;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailCommunicationFileRepository extends FileExpiration<EmailCommunicationFile>, JpaRepository<EmailCommunicationFile, Long> {

    @Query(value = """
                    select ecf
                    from EmailCommunicationFile ecf
                    where ecf.id = :id
                    and ecf.status = 'ACTIVE'
                    and ecf.isReport = false
            """
    )
    Optional<EmailCommunicationFile> findCommunicationFileByIdAndStatus(Long id);

    @Query(value = """
            SELECT ecf.localFileUrl
            FROM EmailCommunicationFile ecf
            WHERE ecf.emailCommunicationId = :emailCommunicationId
            AND ecf.status = 'ACTIVE'
            AND ecf.isReport = false
            """
    )
    List<String> findAllByEmailCommunicationIdAndStatus(Long emailCommunicationId);

    @Query(value = """
                    select ecf
                    from EmailCommunicationFile ecf
                    where ecf.emailCommunicationId = :emailCommunicationId
                    and ecf.status = 'ACTIVE'
                    and ecf.isReport = false
            """
    )
    List<EmailCommunicationFile> findAllActiveFileByEmailCommunicationId(Long emailCommunicationId);

    @Query(value = """
                    select ecf
                    from EmailCommunicationFile ecf
                    where ecf.emailCommunicationId = :emailCommunicationId
                    and ecf.status = :entityStatus
                    and ecf.isReport = true
            """
    )
    Optional<EmailCommunicationFile> findReportFileByEmailCommunicationId(Long emailCommunicationId, EntityStatus entityStatus);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT ef.*
             FROM crm.email_communication_files AS ef
                      CROSS JOIN latest_doc_period dp
             WHERE ef.file_url IS NOT NULL
               AND ef.is_archived
               AND ef.status <> 'DELETED'
               AND CURRENT_DATE > ((ef.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<EmailCommunicationFile> findExpiredFiles();

    @Query("""
            select count(ecf.id) > 0
            from EmailCommunicationFile ecf
            where ecf.id <> :currentEntityId
            and ecf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM crm.email_communication_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<EmailCommunicationFile> findFailedArchivationFiles();

}
