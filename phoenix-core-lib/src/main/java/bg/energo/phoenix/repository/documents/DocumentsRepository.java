package bg.energo.phoenix.repository.documents;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.service.archivation.edms.FileExpiration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentsRepository extends FileExpiration<Document>, JpaRepository<Document, Long> {

    @Query("""
            select doc
            from BillingRun run
            join Invoice inv on run.id = inv.billingId
            join InvoiceDocumentFile docFile on docFile.invoiceId = inv.id
            join Document doc on doc.id = docFile.documentId
            where run.id = :runId
            """)
    List<Document> findBillingRunInvoiceDocuments(Long runId);

    @Query("""
            select doc
            from BillingRun run
            join Invoice inv on run.id = inv.billingId
            join InvoiceDocumentFile docFile on docFile.invoiceId = inv.id
            join Document doc on doc.id = docFile.documentId
            where run.id = :runId
            and inv.invoiceStatus <> 'REAL'
            """)
    List<Document> findBillingRunInvoiceDocumentsIsNotReal(Long runId);

    List<Document> findBySystemUserIdAndStatus(String username, DocumentStatus status);

    @Query(value = """
            select d.* from template.document d
            where  d.document_status = 'UNSIGNED'
              and d.system_user_id = :username
              and d.status = 'ACTIVE'
              and ( coalesce(d.status_modify_date,d.create_date)>= :fromDate)
              and (
                (
                    'SIGNATUS' = ANY(signers)
                        AND NOT 'SIGNATUS' = ANY(d.signed_by)
                    AND not 'SYSTEM_CERTIFICATE' = any(signers)
                    )
                    OR
                    -- Case 2: Signers contain SYSTEM_CERTIFICATE, signed by SYSTEM_CERTIFICATE, signers also contain SIGNATUS and not signed by SIGNATUS
                (
                    'SYSTEM_CERTIFICATE' = ANY(signers)
                        AND 'SYSTEM_CERTIFICATE' = ANY(d.signed_by)
                        AND 'SIGNATUS' = ANY(signers)
                        AND NOT 'SIGNATUS' = ANY(d.signed_by)
                    )
                )
            
            
            """, nativeQuery = true)
    List<Document> findDocumentsForUser(String username, LocalDateTime fromDate);

    @Query("""
            select d
            from ProductContractSignableDocuments csd
                     join Document d on csd.documentId = d.id
            where csd.contractDetailId = :id
              and d.status = 'ACTIVE'
            """)
    List<Document> findSignedDocumentsForProductContractDetail(Long id);

    @Query("""
            select d
            from ServiceContractSignableDocuments csd
                     join Document d on csd.documentId = d.id
            where csd.contractDetailId = :id
              and d.status = 'ACTIVE'
            """)
    List<Document> findSignedDocumentsForServiceContractDetail(Long id);

    @Query("""
            select d
            from ActionSignableDocuments asd
            join Document d on asd.documentId = d.id
            where asd.actionId = :actionId
            and d.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForAction(Long actionId);

    @Query("""
            select d
            from ActionSignableDocuments asd
            join Document d on asd.documentId = d.id
            where asd.serviceContractDetailId = :serviceContractDetailId
            and d.status = 'ACTIVE'
            """)
    List<Document> findActionDocumentsForServiceContractDetail(Long serviceContractDetailId);

    @Query("""
            select d
            from ActionSignableDocuments asd
            join Document d on asd.documentId = d.id
            where asd.productContractDetailId = :productContractDetailId
            and d.status = 'ACTIVE'
            """)
    List<Document> findActionDocumentsForProductContractDetail(Long productContractDetailId);

    @Query("""
            select d
            from DepositDocumentFile df
                     join Document d on df.documentId = d.id
            where df.depositId in :ids
              and d.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForDeposit(List<Long> ids);

    @Query("""
            select d
            from CancellationDcnDocFile cdf
                     join Document d on cdf.documentId = d.id
            where cdf.cancellationId = :id
              and d.status = 'ACTIVE'
              and cdf.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForCancellation(Long id);

    @Query("""
            select d
            from ReschedulingSignableDocuments rd
                     join Document d on rd.documentId = d.id
            where rd.reschedulingId = :id
              and d.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForRescheduling(Long id);

    @Query("""
            select d
            from DisconnectionPowerSupplyRequestsDocuments drd
                     join Document d on drd.documentId = d.id
            where drd.disconnectionPowerSupplyRequestId = :id
              and d.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForDisconnectionRequest(Long id);

    @Query("""
                SELECT d
                FROM Document d
                WHERE d.id IN (:documentIds)
            """)
    List<Document> findDocumentsByIds(@Param("documentIds") List<Long> documentIds);

    @Query("""
            select d
            from ObjectionToChangeOfCbgDocument otdf
                     join Document d on otdf.documentId = d.id
            where otdf.changeOfCbgId = :id
              and d.status = 'ACTIVE'
              and otdf.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForObjectionToCbg(Long id);

    @Query("""
            select d
            from ObjectionWithdrawalToCbgDocFile owdf
                     join Document d on owdf.documentId = d.id
            where owdf.changeOfWithdrawalId = :id
              and d.status = 'ACTIVE'
              and owdf.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForObjWithdrawal(Long id);

    @Query(value = """
            select d.* from  template.document d
            join invoice.invoice_document_files idf on d.id=idf.document_id
            join invoice.invoices inv on inv.id=idf.invoice_id
            where d.status='ACTIVE'
            and inv.billing_id=:billingId
            and ((:excludedInvoiceIds) is null or inv.id not in (:excludedInvoiceIds))
            """, nativeQuery = true)
    Page<Document> findAllByBillingId(Long billingId,
                                      List<Long> excludedInvoiceIds,
                                      Pageable pageable);

    @Query("""
            select d
            from ReconnectionOfThePowerSupplyDocuments ropsd
                     join Document d on ropsd.documentId = d.id
            where ropsd.reconnectionPowerSupplyId = :id
              and d.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForReconnectionRequest(Long id);

    @Query(value = """
            select doc.id + 1
            from template.document doc
            order by doc.id desc
            limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

    @Query(value = """
            WITH latest_doc_period AS (SELECT number_of_months
                                        FROM nomenclature.document_expiration_period
                                        ORDER BY create_date DESC
                                        LIMIT 1)
             SELECT doc.*
             FROM template.document AS doc
                      CROSS JOIN latest_doc_period dp
             WHERE doc.signed_file_url IS NOT NULL
               AND doc.is_archived
               AND doc.status <> 'DELETED'
               AND CURRENT_DATE > ((doc.create_date + INTERVAL '1 month' * dp.number_of_months) - INTERVAL '1 day');
            """, nativeQuery = true)
    List<Document> findExpiredFiles();

    @Query(value = """
            SELECT doc.*
            FROM template.document AS doc
            WHERE (((not doc.is_archived or doc.is_archived IS NULL) and doc.document_status = 'SIGNED'
                and cardinality(doc.signed_by) > 0 and doc.signed_by = doc.signers) or
                   (not doc.is_unsigned_archived or doc.is_unsigned_archived IS NULL))
              AND doc.status != 'DELETED'
              AND (doc.file_id is null or doc.unsigned_file_id is null)
              and (doc.document_id is null or doc.unsigned_document_id is null)
            """, nativeQuery = true)
    List<Document> findFailedArchivationFiles();

    @Query("""
            select count(doc.id) > 0
            from Document doc
            where doc.id <> :currentEntityId
            and doc.signedFileUrl = :fileUrl
            """)
    boolean isFileUsedInOtherEntities(Long currentEntityId, String fileUrl);

    @Query("""
            select doc
            from MLODocumentFile mloDF
            join Document doc 
            on mloDF.documentId = doc.id
            where mloDF.mloId = :id
              and doc.status = 'ACTIVE' 
              and mloDF.status = 'ACTIVE'
            """)
    List<Document> findDocumentsForMloOffsetting(Long id);
}
