package bg.energo.phoenix.repository.crm.smsCommunication;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationFiles;
import bg.energo.phoenix.model.response.crm.smsCommunication.ReportResponse;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsCommunicationFilesRepository extends FileExpiration<SmsCommunicationFiles>, JpaRepository<SmsCommunicationFiles, Long> {
    Optional<SmsCommunicationFiles> findByIdAndStatus(Long id, EntityStatus status);

    @Query("""
                    select scf
                    from SmsCommunicationFiles scf
                    where scf.id=:fileId
                    and scf.status=:entityStatus
                    and scf.isReport=true
            """)
    Optional<SmsCommunicationFiles> findByReportId(Long fileId, EntityStatus entityStatus);

    @Query(value = """
            SELECT ecf.localFileUrl
            FROM SmsCommunicationFiles ecf
            WHERE ecf.smsCommunicationId = :smsCommunicationId
            AND ecf.status = 'ACTIVE'
            """)
    List<String> findAllActiveFileBySmsCommunicationId(Long smsCommunicationId);

    @Query(value = """
            SELECT ecf
            FROM SmsCommunicationFiles ecf
            WHERE ecf.smsCommunicationId = :smsCommunicationId
            AND ecf.status = 'ACTIVE'
            """)
    List<SmsCommunicationFiles> findAllActiveSmsCommunicationFileBySmsCommunicationId(Long smsCommunicationId);

    @Query(value = """
            SELECT ecf
            FROM SmsCommunicationFiles ecf
            WHERE ecf.smsCommunicationId = :smsCommunicationId
            AND ecf.status = 'ACTIVE'
            and ecf.isReport=false
            """)
    List<SmsCommunicationFiles> findAllActiveFileByCommunicationId(Long smsCommunicationId);

    @Query("""
                    select new bg.energo.phoenix.model.response.crm.smsCommunication.ReportResponse(
                                        scf.id,
                                        scf.localFileUrl,
                                        scf.afterSendReport)
                    from SmsCommunicationFiles scf
                    where scf.smsCommunicationId=:smsCommunicationId
                    and scf.status='ACTIVE'
                    and scf.isReport=true
            """)
    List<ReportResponse> findReportsBySmsCommunicationId(Long smsCommunicationId);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT smsf.*
             FROM crm.sms_communication_files AS smsf
                      CROSS JOIN latest_doc_period dp
             WHERE smsf.file_url IS NOT NULL
               AND smsf.is_archived
               AND smsf.status <> 'DELETED'
               AND CURRENT_DATE > ((smsf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<SmsCommunicationFiles> findExpiredFiles();

    @Query("""
            select count(scf.id) > 0
            from SmsCommunicationFiles scf
            where scf.id <> :currentEntityId
            and scf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM crm.sms_communication_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<SmsCommunicationFiles> findFailedArchivationFiles();

}
