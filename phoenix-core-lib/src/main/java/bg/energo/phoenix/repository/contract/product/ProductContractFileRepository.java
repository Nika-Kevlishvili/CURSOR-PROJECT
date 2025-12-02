package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractFile;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractFileRepository extends FileExpiration<ProductContractFile>, JpaRepository<ProductContractFile, Long> {
    @Query("""
            select pcf from ProductContractFile pcf
            where pcf.status = 'ACTIVE'
            and pcf.contractDetailId is null
            """)
    List<ProductContractFile> findActiveByProductContractIdNull();

    List<ProductContractFile> findAllByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    @Query("""
            select pcf
            from ProductContractFile pcf
            where pcf.contractDetailId = :id
            and pcf.status in (:statuses)
            """)
    List<ProductContractFile> findAllByContractDetailIdAndStatusIn(Long id, List<EntityStatus> statuses);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT pcf.*
             FROM product_contract.contract_files AS pcf
                      CROSS JOIN latest_doc_period dp
             WHERE pcf.file_url IS NOT NULL
               AND pcf.is_archived
               AND pcf.status <> 'DELETED'
               AND CURRENT_DATE > ((pcf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<ProductContractFile> findExpiredFiles();

    @Query("""
            select count(pcf.id) > 0
            from ProductContractFile pcf
            where pcf.id <> :currentEntityId
            and pcf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            select doc.id + 1
            from product_contract.contract_files doc
            order by doc.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

    @Query(value = """
            SELECT file.*
            FROM product_contract.contract_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ProductContractFile> findFailedArchivationFiles();

}