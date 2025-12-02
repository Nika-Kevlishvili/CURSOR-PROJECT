package bg.energo.phoenix.repository.contract.activity;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.activity.SystemActivityFile;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemActivityFileRepository extends FileExpiration<SystemActivityFile>, JpaRepository<SystemActivityFile, Long> {

    List<SystemActivityFile> findBySystemActivityIdNullAndStatusIn(List<EntityStatus> statuses);

    List<SystemActivityFile> findAllByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    List<SystemActivityFile> findAllByIdIn(List<Long> ids);

    List<SystemActivityFile> findSystemActivityFileBySystemActivityIdAndStatus(Long systemActivityId, EntityStatus status);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT af.*
             FROM activity.activity_files AS af
                      CROSS JOIN latest_doc_period dp
             WHERE af.file_url IS NOT NULL
               AND af.is_archived
               AND af.status <> 'DELETED'
               AND CURRENT_DATE > ((af.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<SystemActivityFile> findExpiredFiles();

    @Query("""
            select count(saf.id) > 0
            from SystemActivityFile saf
            where saf.id <> :currentEntityId
            and saf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM activity.activity_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<SystemActivityFile> findFailedArchivationFiles();

}
