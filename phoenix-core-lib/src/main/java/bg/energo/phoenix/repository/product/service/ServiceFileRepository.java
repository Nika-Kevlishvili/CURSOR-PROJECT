package bg.energo.phoenix.repository.product.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.service.ServiceFile;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceFileRepository extends FileExpiration<ServiceFile>, JpaRepository<ServiceFile, Long> {
    @Query("""
            select sf from ServiceFile sf
            where sf.status = 'ACTIVE'
            and sf.serviceDetailId is null
            """)
    List<ServiceFile> findActiveByServiceDetailIdNull();

    @Query("""
            select sf from ServiceFile sf
            where sf.id in :ids
            and sf.status in :statuses
            """)
    List<ServiceFile> findAllByIdAndStatusIn(@Param("ids") List<Long> ids, @Param("statuses") List<EntityStatus> statuses);

    @Query("""
            select sf
            from ServiceFile sf
            where sf.serviceDetailId = :id
            and sf.status = 'ACTIVE'
            """)
    List<ServiceFile> findActiveServiceDetailFiles(Long id);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT sf.*
             FROM service.service_files AS sf
                      CROSS JOIN latest_doc_period dp
             WHERE sf.file_url IS NOT NULL
               AND sf.is_archived
               AND sf.status <> 'DELETED'
               AND CURRENT_DATE > ((sf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<ServiceFile> findExpiredFiles();

    @Query("""
            select count(sf.id) > 0
            from ServiceFile sf
            where sf.id <> :currentEntityId
            and sf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM service.service_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ServiceFile> findFailedArchivationFiles();

}
