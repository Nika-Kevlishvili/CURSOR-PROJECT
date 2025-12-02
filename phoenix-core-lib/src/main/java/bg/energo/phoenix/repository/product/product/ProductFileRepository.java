package bg.energo.phoenix.repository.product.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.product.ProductFile;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductFileRepository extends FileExpiration<ProductFile>,JpaRepository<ProductFile, Long> {
    @Query("""
                    Select pf from ProductFile pf where pf.productDetailId is null and pf.status='ACTIVE'
            """)
    List<ProductFile> findActiveByProductDetailIdNull();

    List<ProductFile> findByIdInAndStatusIn(List<Long> ids, List<EntityStatus> statuses);

    @Query("""
            select pf
            from ProductFile pf
            where pf.productDetailId = :id
            and pf.status = 'ACTIVE'
            """)
    List<ProductFile> findActiveProductDetailFiles(Long id);

    @Query(value = """
            WITH latest_doc_period AS (SELECT number_of_months
                                       FROM nomenclature.document_expiration_period
                                       ORDER BY create_date DESC
                                       LIMIT 1)
            SELECT pf.*
            FROM product.product_files AS pf
                     CROSS JOIN latest_doc_period dp
            WHERE pf.file_url IS NOT NULL
              AND pf.is_archived
              AND pf.status <> 'DELETED'
              AND CURRENT_DATE > ((pf.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
           """, nativeQuery = true)
    List<ProductFile> findExpiredFiles();

    @Query("""
            select count(pf.id) > 0
            from ProductFile pf
            where pf.id <> :currentEntityId
            and pf.localFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query(value = """
            SELECT file.*
            FROM product.product_files AS file
            WHERE (not file.is_archived or file.is_archived IS NULL)
              AND file.status != 'DELETED'
              AND file.file_id is null
              and file.document_id is null
            """, nativeQuery = true)
    List<ProductFile> findFailedArchivationFiles();

}
