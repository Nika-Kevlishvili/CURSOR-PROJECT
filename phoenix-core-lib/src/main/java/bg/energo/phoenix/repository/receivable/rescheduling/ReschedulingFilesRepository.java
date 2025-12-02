package bg.energo.phoenix.repository.receivable.rescheduling;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingFiles;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface ReschedulingFilesRepository extends FileExpiration<ReschedulingFiles>, JpaRepository<ReschedulingFiles, Long> {
    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT rf.*
             FROM receivable.rescheduling_files AS rf
                      CROSS JOIN latest_doc_period dp
             WHERE rf.file_url IS NOT NULL
               AND rf.is_archived
               AND rf.status <> 'DELETED'
               AND CURRENT_DATE > ((rf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<ReschedulingFiles> findExpiredFiles();

    @Query("""
                    select rs from ReschedulingFiles rs
                    where rs.id in :ids
                    and rs.status in :statuses
            """)
    Set<ReschedulingFiles> findByIdsAndStatuses(Collection<Long> ids, List<EntityStatus> statuses);

    @Query("""
                    select rf from ReschedulingFiles rf
                    where rf.reschedulingId = :reschedulingId
                    and rf.status = :status
            """)
    Set<ReschedulingFiles> findByReschedulingIdAndStatus(Long reschedulingId, EntityStatus status);

    @Query("""
            select count(rf.id) > 0
            from ReschedulingFiles rf
            where rf.id <> :currentEntityId
            and rf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            select reschedulingFiles.id + 1
            from receivable.rescheduling_files reschedulingFiles
            order by reschedulingFiles.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

    @Query(value = """
            SELECT file.*
            FROM receivable.rescheduling_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ReschedulingFiles> findFailedArchivationFiles();

}
