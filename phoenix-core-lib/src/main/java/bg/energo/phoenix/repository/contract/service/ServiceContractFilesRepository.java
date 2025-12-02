package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractFiles;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceContractFilesRepository extends FileExpiration<ServiceContractFiles>, JpaRepository<ServiceContractFiles,Long> {
    Optional<ServiceContractFiles> findByIdAndStatus(Long id, EntityStatus contractSubObjectStatus);
    List<ServiceContractFiles> findByContractDetailIdAndStatus(Long id, EntityStatus contractSubObjectStatus);
    List<ServiceContractFiles> findServiceContractFilesByContractDetailIdAndStatusIn(Long id,List<EntityStatus> statuses);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT scf.*
             FROM service_contract.contract_files AS scf
                      CROSS JOIN latest_doc_period dp
             WHERE scf.file_url IS NOT NULL
               AND scf.is_archived
               AND scf.status <> 'DELETED'
               AND CURRENT_DATE > ((scf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<ServiceContractFiles> findExpiredFiles();

    @Query("""
            select count(scf.id) > 0
            from ServiceContractFiles scf
            where scf.id <> :currentEntityId
            and scf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            select doc.id + 1
            from service_contract.contract_files doc
            order by doc.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

    @Query(value = """
            SELECT file.*
            FROM service_contract.contract_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ServiceContractFiles> findFailedArchivationFiles();

}
