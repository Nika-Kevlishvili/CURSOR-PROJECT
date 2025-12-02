package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractAdditionalDocuments;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ServiceContractAdditionalDocumentsRepository extends FileExpiration<ServiceContractAdditionalDocuments>, JpaRepository<ServiceContractAdditionalDocuments, Long> {
    Optional<ServiceContractAdditionalDocuments> findByIdAndStatus(Long id, EntityStatus contractSubObjectStatus);

    List<ServiceContractAdditionalDocuments> findServiceContractFilesByContractDetailIdAndStatusIn(Long id, List<EntityStatus> active);

    List<ServiceContractAdditionalDocuments> findByContractDetailIdAndStatus(Long id, EntityStatus contractSubObjectStatus);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT scad.*
             FROM service_contract.contract_additional_docs AS scad
                      CROSS JOIN latest_doc_period dp
             WHERE scad.file_url IS NOT NULL
               AND scad.is_archived
               AND scad.status <> 'DELETED'
               AND CURRENT_DATE > ((scad.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<ServiceContractAdditionalDocuments> findExpiredFiles();

    @Query("""
            select count(scad.id) > 0
            from ServiceContractAdditionalDocuments scad
            where scad.id <> :currentEntityId
            and scad.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM service_contract.contract_additional_docs AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ServiceContractAdditionalDocuments> findFailedArchivationFiles();

}
