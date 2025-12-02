package bg.energo.phoenix.service.signing.qes.repositories;


import bg.energo.phoenix.service.signing.qes.entities.QesDocumentDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QesDocumentDetailsRepository extends JpaRepository<QesDocumentDetails, Long> {

    //Todo change this query
    @Query("""
            select qdd from QesDocumentDetails qdd
            join QesDocument q on qdd.qesDocumentId=q.id
            join Document  doc on doc.id=q.document_id
            where qdd.qesDocumentId in (:documentIds)
            and qdd.isActive=true
            and qdd.status='SIGNED'
            and doc.status='ACTIVE'
            """)
    List<QesDocumentDetails> findByDocumentIds(List<Long> documentIds);

    //Todo change this query
    @Query(value = """
            select qdd.* from template.qes_documents q
                                join template.qes_document_details qdd on qdd.qes_document_id = q.id
                                 join template.document doc on doc.id = q.document_id
                                 join template.templates t on t.id = doc.template_id
                                 join lateral (select string_agg(qd.process_identifier,',') as processId from template.qes_document_details qd where qd.qes_document_id=q.id) as process_id on true
                                 left join lateral (select coalesce(pcsd.contract_detail_id, scsd.contract_detail_id) as contract_detail_id,
                                                           coalesce(pcd.product_detail_id, scd.service_detail_id)     as detail_id,
                                                           coalesce(pd.product_id, sc.service_id)                     as product_id,
                                                           case
                                                               when pd.product_id is null then 'SERVICE'
                                                               when sc.service_id is null then 'PRODUCT' end          as type,
                                                           cd.id                                                      as customer_detail_id,
                                                           cd.name                                                    as customer_name,
                                                           c.identifier                                               as customer_identifier,
                                                           lf.name                                                    as legal_form,
                                                           text(c.customer_number)                                    as customer_number,
                                                           cd.middle_name                                             as customer_middle_name,
                                                           cd.last_name                                               as customer_last_name,
                                                           c.customer_type                                            as customer_type,
                                                           coalesce(pd.global_sales_channel,sc.global_sales_channel)  as global_sale_channel
            
            
                                                    from template.document doc2
                                                             left join product_contract.contract_signable_documents pcsd
                                                                       on pcsd.document_id = doc2.id
                                                             left join product_contract.contract_details pcd on pcd.id = pcsd.contract_detail_id
                                                             left join product.product_details pd on pd.id = pcd.product_detail_id
                                                             left join service_contract.contract_signable_documents scsd
                                                                       on scsd.document_id = doc2.id
                                                             left join service_contract.contract_details scd on scd.id = scsd.contract_detail_id
                                                             left join service.service_details sc on sc.id = scd.service_detail_id
                                                             left join receivable.rescheduling_signable_documents rsd on rsd.document_id=doc2.id
                                                             left join receivable.reschedulings rsc on rsc.id=rsd.rescheduling_id
                                                             join customer.customer_details cd
                                                                  on cd.id = coalesce(scd.customer_detail_id, pcd.customer_detail_id,rsc.customer_detail_id)
                                                             join customer.customers c on c.id = cd.customer_id
                                                             left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                                                    where doc2.id = doc.id
                                                    limit 1) as ctr on true
                        where  doc.status='ACTIVE'
                                 and text(q.status) in (:status)
                          and qdd.is_active=true
                          and qdd.status='SIGNED'
                          and (:excludeIds is null or q.id not in (:excludeIds))
                          and ((:singingStatuses) is null or text(q.signing_status) in (:singingStatuses))
                          and ((:purposes) is null or text(t.template_purpose) in (:purposes))
            
                          and ((coalesce(:productIds, '0') = '0' and coalesce(:serviceIds, '0') = '0') or case
                                                                                                              when ctr.type = 'PRODUCT'
                                                                                                                  then ctr.product_id in (:productIds)
                                                                                                              when ctr.type = 'SERVICE'
                                                                                                                  then ctr.product_id in (:serviceIds) end)
                          and (coalesce(:podIds, '0') = '0' or (ctr.type = 'PRODUCT' and exists(select 1
                                                                                                from product_contract.contract_pods cp
                                                                                                         join pod.pod_details pd on pd.id = cp.pod_detail_id
                                                                                                where cp.contract_detail_id = ctr.detail_id
                                                                                                  and pd.pod_id in (:podIds))))
                          and (coalesce(:updateFrom, '0') = '0' or q.modify_date >= :updateFrom)
                          and (coalesce(:updateTo, '0') = '0' or q.modify_date <= :updateTo)
                          and (coalesce(:createdBy, '0') = '0' or q.system_user_id in (:createdBy))
                          and ( coalesce(doc.status_modify_date ,doc.create_date)>= :fromDate or (q.signing_status='FULLY_SIGNED' and doc.signed_file_url is not null))
                          and (coalesce(:saleChannels, '0') = '0' or (case
                                                                          when ctr.type = 'PRODUCT' then
                                                                              (ctr.global_sale_channel is true or  exists(select 1
                                                                                                                          from product.product_sales_channels psc
                                                                                                                          where psc.product_detail_id = ctr.detail_id
                                                                                                                            and psc.status='ACTIVE'
                                                                                                                            and psc.sales_channel_id in (:saleChannels)))
                                                                          when ctr.type = 'SERVICE' then ( ctr.global_sale_channel is true or  exists(select 1
                                                                                                                                                      from service.service_sales_channels ssc
                                                                                                                                                      where ssc.service_detail_id = ctr.detail_id
                                                                                                                                                        and ssc.status='ACTIVE'
                                                                                                                                                        and ssc.sales_channel_id in (:saleChannels))) end))
                          and (coalesce(:segments, '0') = '0' or (exists(select 1
                                                                         from customer.customer_segments css
                                                                         where css.customer_detail_id = ctr.customer_detail_id
                                                                           and css.segment_id in (:segments))))
                          and (coalesce(:searchFilter, '0') = '0' or (:searchFilter = 'ALL' and (ctr.customer_identifier = :prompt or
                                                                                                 lower(doc.name) like
                                                                                                 lower(('%' || :prompt || '%')) or
                                                                                                 lower(q.identifier) like
                                                                                                 lower(('%' || :prompt || '%')) or
                                                                                                 ctr.customer_number = :prompt or
                                                                                                 lower(ctr.customer_name) like
                                                                                                 lower(('%' || :prompt || '%')))) or
                               (:searchFilter = 'FILE_NAME' and lower(doc.name) like lower(('%' || :prompt || '%'))) or
                               (:searchFilter = 'CUSTOMER_NAME' and lower(ctr.customer_name) like lower(('%' || :prompt || '%'))) or
                               (:searchFilter = 'PROCESS_IDENTIFIER' and process_id.processId like lower(('%' || :prompt || '%'))) or
                               (:searchFilter = 'CUSTOMER_NUMBER' and ctr.customer_number = :prompt) or
                               (:searchFilter = 'CUSTOMER_IDENTIFIER' and lower(ctr.customer_identifier) like lower(('%' || :prompt || '%'))))
            """,nativeQuery = true)
    List<QesDocumentDetails> findByDocumentIdsAndFilter(
                                     List<Long> excludeIds,
                                     List<String> status,
                                     List<String> singingStatuses,
                                     List<String> purposes,

                                     List<Long> productIds,
                                     List<Long> serviceIds,
                                     List<Long> podIds,
                                     List<Long> segments,
                                     List<Long> saleChannels,
                                     List<String> createdBy,
                                     LocalDateTime updateFrom,
                                     LocalDateTime updateTo,
                                     String searchFilter,
                                     String prompt);

    @Query("""
            select count(1)>0 from QesDocument qd
            join Document  doc on doc.id=qd.document_id
            where qd.id in (:documentIds)
            and (qd.signingStatus = 'FULLY_SIGNED' or (qd.signingStatus <> 'FULLY_SIGNED' and exists(select 1 from QesDocumentDetails qdd where qdd.qesDocumentId=qd.id and qdd.status='IN_PROGRESS' and qdd.isActive is true )))
            and doc.status='ACTIVE'
            """)
    boolean findInvalidDocuments(List<Long> documentIds);

    @Query(value = """
select count(1)>0
from  template.qes_documents q
          join template.qes_document_details qdd on qdd.qes_document_id = q.id
          join template.document doc on doc.id = q.document_id
          join template.templates t on t.id = doc.template_id
          join lateral (select string_agg(qd.process_identifier,',') as processId from template.qes_document_details qd where qd.qes_document_id=q.id) as process_id on true
          left join lateral (select coalesce(pcsd.contract_detail_id, scsd.contract_detail_id) as contract_detail_id,
                                    coalesce(pcd.product_detail_id, scd.service_detail_id)     as detail_id,
                                    coalesce(pd.product_id, sc.service_id)                     as product_id,
                                    case
                                        when pd.product_id is null then 'SERVICE'
                                        when sc.service_id is null then 'PRODUCT' end          as type,
                                    cd.id                                                      as customer_detail_id,
                                    cd.name                                                    as customer_name,
                                    c.identifier                                               as customer_identifier,
                                    lf.name                                                    as legal_form,
                                    text(c.customer_number)                                    as customer_number,
                                    cd.middle_name                                             as customer_middle_name,
                                    cd.last_name                                               as customer_last_name,
                                    c.customer_type                                            as customer_type,
                                    coalesce(pd.global_sales_channel,sc.global_sales_channel)  as global_sale_channel


                             from template.document doc2
                                      left join product_contract.contract_signable_documents pcsd
                                                on pcsd.document_id = doc2.id
                                      left join product_contract.contract_details pcd on pcd.id = pcsd.contract_detail_id
                                      left join product.product_details pd on pd.id = pcd.product_detail_id
                                      left join service_contract.contract_signable_documents scsd
                                                on scsd.document_id = doc2.id
                                      left join service_contract.contract_details scd on scd.id = scsd.contract_detail_id
                                      left join service.service_details sc on sc.id = scd.service_detail_id
                                      left join receivable.rescheduling_signable_documents rsd on rsd.document_id=doc2.id
                                      left join receivable.reschedulings rsc on rsc.id=rsd.rescheduling_id
                                      join customer.customer_details cd
                                           on cd.id = coalesce(scd.customer_detail_id, pcd.customer_detail_id,rsc.customer_detail_id)
                                      join customer.customers c on c.id = cd.customer_id
                                      left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                             where doc2.id = doc.id
                             limit 1) as ctr on true
where  doc.status='ACTIVE'
  and text(q.status) in (:status)
  and (:excludedIds is null or q.id not in (:excludedIds))
  and ((:singingStatuses) is null or text(q.signing_status) in (:singingStatuses))
  and ( coalesce(doc.status_modify_date,doc.create_date) >= :fromDate)
  and ((:purposes) is null or text(t.template_purpose) in (:purposes))

  and ((coalesce(:productIds, '0') = '0' and coalesce(:serviceIds, '0') = '0') or case
                                                                                      when ctr.type = 'PRODUCT'
                                                                                          then ctr.product_id in (:productIds)
                                                                                      when ctr.type = 'SERVICE'
                                                                                          then ctr.product_id in (:serviceIds) end)
  and (coalesce(:podIds, '0') = '0' or (ctr.type = 'PRODUCT' and exists(select 1
                                                                        from product_contract.contract_pods cp
                                                                                 join pod.pod_details pd on pd.id = cp.pod_detail_id
                                                                        where cp.contract_detail_id = ctr.detail_id
                                                                          and pd.pod_id in (:podIds))))
  and (coalesce(:updateFrom, '0') = '0' or q.modify_date >= :updateFrom)
  and (coalesce(:updateTo, '0') = '0' or q.modify_date <= :updateTo)
  and (coalesce(:createdBy, '0') = '0' or q.system_user_id in (:createdBy))
  and (coalesce(:saleChannels, '0') = '0' or (case
                                                  when ctr.type = 'PRODUCT' then
                                                      (ctr.global_sale_channel is true or  exists(select 1
                                                                                                  from product.product_sales_channels psc
                                                                                                  where psc.product_detail_id = ctr.detail_id
                                                                                                    and psc.status='ACTIVE'
                                                                                                    and psc.sales_channel_id in (:saleChannels)))
                                                  when ctr.type = 'SERVICE' then ( ctr.global_sale_channel is true or  exists(select 1
                                                                                                                              from service.service_sales_channels ssc
                                                                                                                              where ssc.service_detail_id = ctr.detail_id
                                                                                                                                and ssc.status='ACTIVE'
                                                                                                                                and ssc.sales_channel_id in (:saleChannels))) end))
  and (coalesce(:segments, '0') = '0' or (exists(select 1
                                                 from customer.customer_segments css
                                                 where css.customer_detail_id = ctr.customer_detail_id
                                                   and css.segment_id in (:segments))))
  and (coalesce(:searchFilter, '0') = '0' or (:searchFilter = 'ALL' and (ctr.customer_identifier = :prompt or
                                                                         lower(doc.name) like
                                                                         lower(('%' || :prompt || '%')) or
                                                                         lower(q.identifier) like
                                                                         lower(('%' || :prompt || '%')) or
                                                                         ctr.customer_number = :prompt or
                                                                         lower(ctr.customer_name) like
                                                                         lower(('%' || :prompt || '%')))) or
       (:searchFilter = 'FILE_NAME' and lower(doc.name) like lower(('%' || :prompt || '%'))) or
       (:searchFilter = 'CUSTOMER_NAME' and lower(ctr.customer_name) like lower(('%' || :prompt || '%'))) or
       (:searchFilter = 'PROCESS_IDENTIFIER' and process_id.processId like lower(('%' || :prompt || '%'))) or
       (:searchFilter = 'CUSTOMER_NUMBER' and ctr.customer_number = :prompt) or
       (:searchFilter = 'CUSTOMER_IDENTIFIER' and lower(ctr.customer_identifier) like lower(('%' || :prompt || '%'))))
  and (q.signing_status = 'FULLY_SIGNED' or (q.signing_status <> 'FULLY_SIGNED' and exists(select 1 from template.qes_document_details qdd2 where qdd2.qes_document_id=q.id and qdd2.status='IN_PROGRESS' and qdd2.is_active is true )))
  and doc.status='ACTIVE'
            """,nativeQuery = true)
    boolean findInvalidDocumentsWithFilter(List<Long> excludedIds,
                                           List<String> status,
                                           List<String> singingStatuses,
                                           List<String> purposes,

                                           List<Long> productIds,
                                           List<Long> serviceIds,
                                           List<Long> podIds,
                                           List<Long> segments,
                                           List<Long> saleChannels,
                                           List<String> createdBy,
                                           LocalDateTime updateFrom,
                                           LocalDateTime updateTo,
                                           String searchFilter,
                                           String prompt,
                                           LocalDateTime fromDate);

    @Query("""
            select count(1)>0 from QesDocument qdd
            join Document  doc on doc.id=qdd.document_id
            
            where  qdd.id in (:documentIds)
            and qdd.signingStatus = 'TO_BE_SIGNED'
            and doc.status='ACTIVE'
            """)
    boolean findInvalidDocumentsForCancel(List<Long> documentIds);

    @Query(value = """
            select count(1)>0 from template.qes_documents q
                                 join template.document doc on doc.id = q.document_id
                                 join template.templates t on t.id = doc.template_id
                                 join lateral (select string_agg(qd.process_identifier,',') as processId from template.qes_document_details qd where qd.qes_document_id=q.id) as process_id on true
                                 left join lateral (select coalesce(pcsd.contract_detail_id, scsd.contract_detail_id) as contract_detail_id,
                                                           coalesce(pcd.product_detail_id, scd.service_detail_id)     as detail_id,
                                                           coalesce(pd.product_id, sc.service_id)                     as product_id,
                                                           case
                                                               when pd.product_id is null then 'SERVICE'
                                                               when sc.service_id is null then 'PRODUCT' end          as type,
                                                           cd.id                                                      as customer_detail_id,
                                                           cd.name                                                    as customer_name,
                                                           c.identifier                                               as customer_identifier,
                                                           lf.name                                                    as legal_form,
                                                           text(c.customer_number)                                    as customer_number,
                                                           cd.middle_name                                             as customer_middle_name,
                                                           cd.last_name                                               as customer_last_name,
                                                           c.customer_type                                            as customer_type,
                                                           coalesce(pd.global_sales_channel,sc.global_sales_channel)  as global_sale_channel
            
            
                                                    from template.document doc2
                                                             left join product_contract.contract_signable_documents pcsd
                                                                       on pcsd.document_id = doc2.id
                                                             left join product_contract.contract_details pcd on pcd.id = pcsd.contract_detail_id
                                                             left join product.product_details pd on pd.id = pcd.product_detail_id
                                                             left join service_contract.contract_signable_documents scsd
                                                                       on scsd.document_id = doc2.id
                                                             left join service_contract.contract_details scd on scd.id = scsd.contract_detail_id
                                                             left join service.service_details sc on sc.id = scd.service_detail_id
                                                             left join receivable.rescheduling_signable_documents rsd on rsd.document_id=doc2.id
                                                             left join receivable.reschedulings rsc on rsc.id=rsd.rescheduling_id
                                                             join customer.customer_details cd
                                                                  on cd.id = coalesce(scd.customer_detail_id, pcd.customer_detail_id,rsc.customer_detail_id)
                                                             join customer.customers c on c.id = cd.customer_id
                                                             left join nomenclature.legal_forms lf on lf.id = cd.legal_form_id
                                                    where doc2.id = doc.id
                                                    limit 1) as ctr on true
                        where  doc.status='ACTIVE'
                                 and text(q.status) in (:status)
                          and (:excludedIds is null or q.id not in (:excludedIds))
                          and ((:singingStatuses) is null or text(q.signing_status) in (:singingStatuses))
                          and ((:purposes) is null or text(t.template_purpose) in (:purposes))
            
                          and ((coalesce(:productIds, '0') = '0' and coalesce(:serviceIds, '0') = '0') or case
                                                                                                              when ctr.type = 'PRODUCT'
                                                                                                                  then ctr.product_id in (:productIds)
                                                                                                              when ctr.type = 'SERVICE'
                                                                                                                  then ctr.product_id in (:serviceIds) end)
                          and (coalesce(:podIds, '0') = '0' or (ctr.type = 'PRODUCT' and exists(select 1
                                                                                                from product_contract.contract_pods cp
                                                                                                         join pod.pod_details pd on pd.id = cp.pod_detail_id
                                                                                                where cp.contract_detail_id = ctr.detail_id
                                                                                                  and pd.pod_id in (:podIds))))
                          and (coalesce(:updateFrom, '0') = '0' or q.modify_date >= :updateFrom)
                          and (coalesce(:updateTo, '0') = '0' or q.modify_date <= :updateTo)
                          and (coalesce(:createdBy, '0') = '0' or q.system_user_id in (:createdBy))
                          and (coalesce(:saleChannels, '0') = '0' or (case
                                                                          when ctr.type = 'PRODUCT' then
                                                                              (ctr.global_sale_channel is true or  exists(select 1
                                                                                                                          from product.product_sales_channels psc
                                                                                                                          where psc.product_detail_id = ctr.detail_id
                                                                                                                            and psc.status='ACTIVE'
                                                                                                                            and psc.sales_channel_id in (:saleChannels)))
                                                                          when ctr.type = 'SERVICE' then ( ctr.global_sale_channel is true or  exists(select 1
                                                                                                                                                      from service.service_sales_channels ssc
                                                                                                                                                      where ssc.service_detail_id = ctr.detail_id
                                                                                                                                                        and ssc.status='ACTIVE'
                                                                                                                                                        and ssc.sales_channel_id in (:saleChannels))) end))
                          and (coalesce(:segments, '0') = '0' or (exists(select 1
                                                                         from customer.customer_segments css
                                                                         where css.customer_detail_id = ctr.customer_detail_id
                                                                           and css.segment_id in (:segments))))
                          and (coalesce(:searchFilter, '0') = '0' or (:searchFilter = 'ALL' and (ctr.customer_identifier = :prompt or
                                                                                                 lower(doc.name) like
                                                                                                 lower(('%' || :prompt || '%')) or
                                                                                                 lower(q.identifier) like
                                                                                                 lower(('%' || :prompt || '%')) or
                                                                                                 ctr.customer_number = :prompt or
                                                                                                 lower(ctr.customer_name) like
                                                                                                 lower(('%' || :prompt || '%')))) or
                               (:searchFilter = 'FILE_NAME' and lower(doc.name) like lower(('%' || :prompt || '%'))) or
                               (:searchFilter = 'CUSTOMER_NAME' and lower(ctr.customer_name) like lower(('%' || :prompt || '%'))) or
                               (:searchFilter = 'PROCESS_IDENTIFIER' and process_id.processId like lower(('%' || :prompt || '%'))) or
                               (:searchFilter = 'CUSTOMER_NUMBER' and ctr.customer_number = :prompt) or
                               (:searchFilter = 'CUSTOMER_IDENTIFIER' and lower(ctr.customer_identifier) like lower(('%' || :prompt || '%'))))
            and q.signing_status = 'TO_BE_SIGNED'
            """,nativeQuery = true)
    boolean findInvalidDocumentToCancelFilter(
            List<Long> excludedIds,
            List<String> status,
            List<String> singingStatuses,
            List<String> purposes,

            List<Long> productIds,
            List<Long> serviceIds,
            List<Long> podIds,
            List<Long> segments,
            List<Long> saleChannels,
            List<String> createdBy,
            LocalDateTime updateFrom,
            LocalDateTime updateTo,
            String searchFilter,
            String prompt
    );

    @Modifying
    @Query("""
    DELETE FROM QesDocumentDetails q
    WHERE q.qesDocumentId IN (:qesDocumentIds)
    AND q.createDate <> (
        SELECT MIN(q2.createDate)
        FROM QesDocumentDetails q2
        WHERE q2.qesDocumentId = q.qesDocumentId
    )
""")
    void deleteAllExceptOldestByQesDocumentIds(@Param("qesDocumentIds") List<Long> qesDocumentIds);



    @Query("""
    SELECT q
    FROM QesDocumentDetails q
    WHERE q.qesDocumentId IN (:qesDocumentIds)
""")
    List<QesDocumentDetails> findByQesDocumentIds(@Param("qesDocumentIds") List<Long> qesDocumentIds);

    @Query(value = """
                select nextval('template.qes_process_identifier_seq')
            """,nativeQuery = true)
    Long nextProcessId();

    @Query("""
            select qdd from QesDocumentDetails qdd 
            where qdd.qesDocumentId in (:sessionDocuments)
            and qdd.status='SIGNED'
            and qdd.createDate = (select max(qdd2.createDate) from QesDocumentDetails qdd2 where qdd2.qesDocumentId=qdd.qesDocumentId and qdd2.status='SIGNED')
            """)
    List<QesDocumentDetails> findNewestSignedDocs(List<Long> sessionDocuments);
}
