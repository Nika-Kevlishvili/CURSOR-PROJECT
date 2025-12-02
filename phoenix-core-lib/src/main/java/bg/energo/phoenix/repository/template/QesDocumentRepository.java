package bg.energo.phoenix.repository.template;

import bg.energo.phoenix.model.entity.template.QesDocument;
import bg.energo.phoenix.model.response.template.QesDocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QesDocumentRepository extends JpaRepository<QesDocument, Long> {


    @Query(value = """
            select
                q.id                                                                                             as documentId,
                doc.name                                                                                         as fileName,
                process_id.processId as fileIdentifier,
                ctr.customer_type                                                                                   as customerType,
                ctr.legal_form                                                                                      as legalFormName,
                ctr.customer_name                                                                                   as customerName,
                ctr.customer_middle_name                                                                            as middleName,
                ctr.customer_last_name                                                                              as lastName,
                ctr.customer_identifier                                                                             as customerIdentifier,
                t.template_purpose                                                                                  as templatePurpose,
                case when ctr.type='PRODUCT' then
                         coalesce((select CASE when max(total_count) > 3 then concat(string_agg(spssCN.saleChannelName,','),'...') else string_agg(spssCN.saleChannelName,',') end
                                   from (select text(sc.name) as saleChannelName, COUNT(*) OVER () AS total_count
                                         from nomenclature.sales_channels sc
                                                  join product.product_sales_channels psc
                                                       on sc.id = psc.sales_channel_id and ctr.detail_id=psc.product_detail_id where psc.status='ACTIVE' limit 3) as spssCN),'ALL')
                     when ctr.type = 'SERVICE' then coalesce((select CASE when max(total_count) > 3 then concat(string_agg(spssCN.saleChannelName,','),'...') else string_agg(spssCN.saleChannelName,',') end
                                                              from (select sc.name as saleChannelName, COUNT(*) OVER () AS total_count
                                                                    from nomenclature.sales_channels sc
                                                                             join service.service_sales_channels ssc
                                                                                  on sc.id = ssc.sales_channel_id and ctr.detail_id = ssc.service_detail_id and
                                                                                     ctr.type = 'SERVICE' where ssc.status='ACTIVE' limit 3) as spssCN),'ALL'    ) end
                    as salesChannel,
                q.quantity_to_sign                                                                                  as quantityToSign,
                q.signed_quantity                                                                                   as signedQuantity,
                q.status                                                                                   as status,
                q.signing_status                                                                                    as signingStatus,
                q.modify_date                                                                                       as updateTime,
                ctr.type                                                                                            as type
            from template.qes_documents q
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
              and ((:singingStatuses) is null or text(q.signing_status) in (:singingStatuses))
              and ((:purposes) is null or text(t.template_purpose) in (:purposes))
            
              and (((:productIds) is null and (:serviceIds) is null) or case
                                                                                                  when ctr.type = 'PRODUCT'
                                                                                                      then ctr.product_id in (:productIds)
                                                                                                  when ctr.type = 'SERVICE'
                                                                                                      then ctr.product_id in (:serviceIds) end)
              and ((:podIds) is null or (ctr.type = 'PRODUCT' and exists(select 1
                                                                                    from product_contract.contract_pods cp
                                                                                             join pod.pod_details pd on pd.id = cp.pod_detail_id
                                                                                    where cp.contract_detail_id = ctr.detail_id
                                                                                      and pd.pod_id in (:podIds))))
              and (coalesce(:updateFrom, '0') = '0' or q.modify_date >= :updateFrom)
              and (coalesce(:updateTo, '0') = '0' or q.modify_date <= :updateTo)
              and ( coalesce(doc.status_modify_date ,doc.create_date)>= :fromDate or (q.signing_status='FULLY_SIGNED' and doc.signed_file_url is not null))
              and ((:createdBy) is null or q.system_user_id in (:createdBy))
              and (:saleChannelProvided is false or (case
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
              and ((:segments) is null or (exists(select 1
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
            """, nativeQuery = true,
            countQuery = """
                              select count(1)
                              from template.qes_documents q
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
                              where doc.status='ACTIVE'
                                      and text(q.status) in (:status)
                                and ((:singingStatuses) is null or text(q.signing_status) in (:singingStatuses))
                                and ((:purposes) is null or text(t.template_purpose) in (:purposes))
                                and (((:productIds) is null and (:serviceIds) is null) or case
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
                                and (( coalesce(doc.status_modify_date ,doc.create_date) >= :fromDate) or (q.signing_status='FULLY_SIGNED' and doc.signed_file_url is not null))
                                and ((:createdBy) is null or q.system_user_id in (:createdBy))
                    and (:saleChannelProvided is false or (case
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
                    and ((:segments) is null or (exists(select 1
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
                    """)
    Page<QesDocumentResponse> filter(List<String> status,
                                     List<String> singingStatuses,
                                     List<String> purposes,

                                     List<Long> productIds,
                                     List<Long> serviceIds,
                                     List<Long> podIds,
                                     List<Long> segments,
                                     List<Long> saleChannels,
                                     boolean saleChannelProvided,
                                     List<String> createdBy,
                                     LocalDateTime updateFrom,
                                     LocalDateTime updateTo,
                                     String searchFilter,
                                     String prompt,
                                     LocalDateTime fromDate,
                                     Pageable pageable);

    @Query("""
                SELECT q
                FROM QesDocument q
                WHERE  q.id IN (:qesDocumentIds)
            """)
    List<QesDocument> findDocumentIdsByRequest(
            @Param("qesDocumentIds") List<Long> qesDocumentIds
    );

    @Query(value = """
                select q.* from template.qes_documents q
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
                          and (( coalesce(doc.status_modify_date ,doc.create_date) >= :fromDate) or (q.signing_status='FULLY_SIGNED' and doc.signed_file_url is not null))
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
    List<QesDocument> findQesDocumentsByFilter(
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
            String prompt,
            LocalDateTime fromDate
    );

    @Query("""
                SELECT q
                FROM QesDocument q
                WHERE q.id IN (:qesDocumentIds)
            """)
    List<QesDocument> findAllByIds(@Param("qesDocumentIds") List<Long> qesDocumentIds);
}
