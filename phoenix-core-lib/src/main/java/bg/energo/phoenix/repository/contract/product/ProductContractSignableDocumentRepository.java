package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContractSignableDocuments;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.response.contract.ContractDocumentEmailResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductContractSignableDocumentRepository extends JpaRepository<ProductContractSignableDocuments, Long> {

    @Query(value = """
            select d from Document d
            where d.id in (select csd.documentId
                           from ProductContractSignableDocuments csd
                           where csd.contractDetailId = :id
                           and d.templateId in (:templateIds)
                           and d.status = 'ACTIVE'
                           and csd.documentId = d.id
                            and (cast(d.fileFormat as string) <> 'PDF'
                                    or cast(d.signers as string) in ('{NO}', '{}') or cast(d.signedBy as string) in ('{}') )
                           )
            """)
    List<Document> getDocumentPreviousFiles(Long id, List<Long> templateIds);

    @Query(nativeQuery = true, value = """
            with filtered_documents as (select csd.contract_detail_id             as cd_id,
                                               string_agg(text(csd.id), ' ')      as doc_ids,
                                               string_agg(d.signed_file_url, '&') as doc_urls,
                                               string_agg(d.name, '&')            as doc_names
                                        from product_contract.contract_signable_documents csd
                                                 join template.document d on d.id = csd.document_id
                                        where csd.status = 'ACTIVE'
                                          and (csd.email_sent is null or csd.email_sent = false)
                                          and d.document_status = 'SIGNED'
                                          and d.status = 'ACTIVE'
                                        group by csd.contract_detail_id),
                 latest_template_details as (select td.template_id,
                                                    td.subject,
                                                    td.start_date
                                             from template.template_details td
                                                      inner join (select template_id,
                                                                         max(start_date) as max_start_date
                                                                  from template.template_details
                                                                  where start_date <= current_date
                                                                  group by template_id) max_dates
                                                                 on td.template_id = max_dates.template_id and
                                                                    td.start_date = max_dates.max_start_date),

                 default_mailbox as (select id
                                     from nomenclature.email_mailboxes
                                     where is_default = true
                                       and status = 'ACTIVE'
                                     limit 1)

            select fd.doc_ids                         as contractdocumentids,
                   fd.doc_urls                        as documenturls,
                   fd.doc_names                       as documentnames,
                   temp.id                            as emailtemplateid,
                   ltd.subject                        as emailsubject,
                   dm.id                              as mailboxid,
                   ccc.customer_communication_id      as customercommunicationid,
                   string_agg(ccc.contact_value, ';') as emails,
                   cust_det.id                        as customerdetailid,
                   c.id                               as contractid,
                   cd.version_id                      as contractversion
            from product_contract.contracts c
                     join product_contract.contract_details cd on c.id = cd.contract_id
                     join filtered_documents fd on cd.id = fd.cd_id
                     join customer.customer_details cust_det on cd.customer_detail_id = cust_det.id
                     join product.product_details pd on cd.product_detail_id = pd.id
                     join product.product_templates pt on pd.id = pt.product_detail_id
                and pt.status = 'ACTIVE'
                and pt.product_template_type = 'EMAIL_TEMPLATE'
                     join template.templates temp on pt.template_id = temp.id
                     join latest_template_details ltd on ltd.template_id = temp.id
                     join customer.customer_communication_contacts ccc
                          on ccc.customer_communication_id = cd.customer_communication_id_for_contract
                              and ccc.status = 'ACTIVE'
                              and ccc.contact_type = 'EMAIL'
                     cross join default_mailbox dm
            group by fd.doc_ids,
                     fd.doc_urls,
                     fd.doc_names,
                     temp.id,
                     ltd.subject,
                     dm.id,
                     ccc.customer_communication_id,
                     cust_det.id,
                     c.id,
                     cd.version_id
            """)
    List<ContractDocumentEmailResponse> fetchContractAndDocumentsToSendEmail();

    @Modifying
    @Query("""
            update ProductContractSignableDocuments d
            set d.emailSent = true
            where d.id in (:documentIdsToUpdate)
            """)
    void updateDocumentsForSentEmails(Set<Long> documentIdsToUpdate);

    @Query("""
            select d from Document d
            where d.id in (select csd.documentId
                           from ProductContractSignableDocuments csd
                           where csd.contractDetailId = :detailId
                           and d.status = 'ACTIVE'
                           and csd.documentId = d.id)
            """)
    List<Document> getDocumentsForContractByContractDetailId(Long detailId);
}
