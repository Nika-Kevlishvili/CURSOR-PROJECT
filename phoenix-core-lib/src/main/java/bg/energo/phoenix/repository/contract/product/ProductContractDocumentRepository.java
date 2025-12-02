package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDocument;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractDocumentRepository extends FileExpiration<ProductContractDocument>, JpaRepository<ProductContractDocument, Long> {
    @Query("""
            select pcd from ProductContractDocument pcd
            where pcd.contractDetailId is null and pcd.status = 'ACTIVE'
            """)
    List<ProductContractDocument> findActiveByProductContractIdNull();

    List<ProductContractDocument> findAllByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    @Query("""
            select pcd from ProductContractDocument pcd
            where pcd.contractDetailId = :id
            and pcd.status in(:statuses)
            """)
    List<ProductContractDocument> findAllByContractDetailIdAndStatusIn(Long id, List<EntityStatus> statuses);

    @Query(value = """
             WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT pcad.*
             FROM product_contract.contract_additional_docs AS pcad
                      CROSS JOIN latest_doc_period dp
             WHERE pcad.file_url IS NOT NULL
               AND pcad.is_archived
               AND pcad.status <> 'DELETED'
               AND CURRENT_DATE > ((pcad.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<ProductContractDocument> findExpiredFiles();

    @Query("""
            select count(pcd.id) > 0
            from ProductContractDocument pcd
            where pcd.id <> :currentEntityId
            and pcd.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM product_contract.contract_additional_docs AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ProductContractDocument> findFailedArchivationFiles();

}