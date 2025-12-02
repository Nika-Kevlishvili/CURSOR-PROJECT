package bg.energo.phoenix.repository.template;

import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.enums.template.ContractTemplateLanguage;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.model.response.template.TemplateListingMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    Optional<ContractTemplate> findByTemplatePurposeAndDefaultForLatePaymentFineEmail(
            ContractTemplatePurposes purpose,
            Boolean defaultForLatePaymentFineEmail
    );

    Optional<ContractTemplate> findByTemplatePurposeAndDefaultForLatePaymentFineDocument(
            ContractTemplatePurposes purpose,
            Boolean DefaultForLatePaymentFineDocument
    );

    @Query("""
            select ct.id from ContractTemplate ct
            where ct.id in (:templateIds)
            and ct.status= :entityStatus
            and ct.templatePurpose=:purpose
            """)
    Set<Long> findAllIdByIdAndStatusAndPurpose(Set<Long> templateIds, ContractTemplatePurposes purpose, ContractTemplateStatus entityStatus);

    @Query("""
            select ct.id from ContractTemplate ct
            join ContractTemplateDetail ctd on ctd.templateId=ct.id
            where ct.id in (:templateIds)
            and ct.status= :entityStatus
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            and ct.templatePurpose=:purpose
            and ctd.language in (:languages)
            and ctd.templateType in (:templateTypes)
            """)
    Set<Long> findAllIdByIdAndLanguages(Collection<Long> templateIds, ContractTemplatePurposes purpose, List<ContractTemplateLanguage> languages, List<ContractTemplateType> templateTypes, ContractTemplateStatus entityStatus, LocalDate currentDate);
@Query("""
         select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,text(ctd.outputFileFormat),text(ctd.fileSigning))
            from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate  ct on ct.id=ctd.templateId
            where ct.status = 'ACTIVE'
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = ctd.templateId and ctd2.startDate <=:currentDate)
            and (lower(ctd.name) like :prompt or text(ct.id) like :prompt)
            and ctd.templateType in (:types)
            and ctd.language in (:languages)
            and ct.templatePurpose = :templatePurpose
            order by ct.id desc
        """)
    Page<ContractTemplateShortResponse> findAvailable(String prompt,
                                                      ContractTemplatePurposes templatePurpose,
                                                      LocalDate currentDate,
                                                      List<ContractTemplateLanguage> languages,
                                                      List<ContractTemplateType> types,
                                                      PageRequest pageRequest);

    Optional<ContractTemplate> findByIdAndStatusIn(Long id, List<ContractTemplateStatus> status);


    @Query(nativeQuery = true, value = """
            Select distinct a.name
            from (Select 'objection to change of cbg' as name
                  from receivable.objection_to_change_of_cbg_templates a
                           left join receivable.objection_to_change_of_cbg b on a.objection_to_change_of_cbg_id = b.id
                  where b.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'objection withdrawal to change of cbg'
                  from receivable.objection_withdrawal_to_change_of_cbg_templates a
                           left join receivable.objection_withdrawal_to_change_of_cbg b
                                     on a.objection_withdrawal_to_change_of_cbg_id = b.id
                  where b.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'power supply dcn cancellations'
                  from receivable.power_supply_dcn_cancellation_templates a
                           left join receivable.power_supply_dcn_cancellations b on a.power_supply_dcn_cancellation_id = b.id
                  where b.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'power supply disconnection reminders'
                  from receivable.power_supply_disconnection_reminder_templates a
                           left join receivable.power_supply_disconnection_reminders b
                                     on a.power_supply_disconnection_reminder_id = b.id
                  where b.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'power supply disconnection requests'
                  from receivable.power_supply_disconnection_request_templates a
                           left join receivable.power_supply_disconnection_requests b
                                     on a.power_supply_disconnection_request_id = b.id
                  where b.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'power supply reconnections'
                  from receivable.power_supply_reconnection_templates a
                           left join receivable.power_supply_reconnections b on a.power_supply_reconnection_id = b.id
                  where b.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'billings'
                  from billing.billings
                  where status <> 'DELETED'
                    and (template_id = :templateId
                      or email_template_id = :templateId)

                  union all

                  Select 'penalties'
                  from terms.penalties
                  where status = 'ACTIVE'
                    and (template_id = :templateId
                      or email_template_id = :templateId)

                  union all

                  Select 'product'
                  from product.product_templates a
                           left join product.product_details b on a.product_detail_id = b.id
                           left join product.products p on b.product_id = p.id
                  where p.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'reminders'
                  from receivable.reminders
                  where status = 'ACTIVE'
                    and template_id = :templateId

                  union all

                  Select 'service'
                  from service.service_templates a
                           left join service.service_details b on a.service_detail_id = b.id
                           left join service.services s on b.service_id = s.id
                  where s.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  Select 'sms communications'
                  from crm.sms_communications
                  where status = 'ACTIVE'
                    and template_id = :templateId

                  union all

                  Select 'terminations'
                  from product.termination_templates a
                           left join product.terminations b on a.termination_id = b.id
                  where b.status = 'ACTIVE'
                    and a.template_id = :templateId

                  union all

                  select 'goods order'
                  from goods_order.orders
                  where status = 'ACTIVE'
                    and (invoice_template_id = :templateId
                      or email_template_id = :templateId)

                  union all

                  select 'service order'
                  from service_order.orders
                  where status = 'ACTIVE'
                    and (invoice_template_id = :templateId
                      or email_template_id = :templateId)) as a
            limit 1
            """)
    Optional<String> findConnectionToAnyObject(@Param("templateId") Long id);

    @Query(nativeQuery = true, value = """
            select td.name                                  as name,
                   td.type                                  as type,
                   t.template_purpose                       as purpose,
                   text(td.file_signing)                    as fileSignings,
                   text(td.output_file_format)              as outputFileFormats,
                   td.language                              as language,
                   t.create_date                            as createDate,
                   t.id                                     as id,
                   t.status                                 as status,
                   t.default_for_goods_order_document       as defaultForGoodsOrderDocument,
                   t.default_for_goods_order_email          as defaultForGoodsOrderEmail,
                   t.default_for_late_payment_fine_document as defaultForLatePaymentFineDocument,
                   t.default_for_late_payment_fine_email    as defaultForLatePaymentFineEmail
            from template.templates t
                     join template.template_details td on t.id = td.template_id
                and t.id in (select innerT.id
                             from template.templates innerT
                                      join template.template_details innerTD on innerT.id = innerTD.template_id
                             where ((:types) is null or text(innerTD.type) in (:types))
                               and ((:outputFileFormats) is null or
                                    cast(innerTD.output_file_format as text[]) && cast(:outputFileFormats as text[]))
                               and ((:templatePurposes) is null or text(innerT.template_purpose) in (:templatePurposes))
                               and ((:fileSignings) is null or
                                    cast(innerTD.file_signing as text[]) && cast(:fileSignings as text[]))
                               and ((:customerTypes) is null or
                                    cast(innerTD.customer_type as text[]) && cast(:customerTypes as text[]))
                               and ((:consumptionPurposes) is null or
                                    cast(innerTD.purpose_of_consumption as text[]) && cast(:consumptionPurposes as text[]))
                               and ((:languages) is null or text(innerTD.language) in (:languages))
                               and (
                                 (:defaultGoodsOrderDocument is not null and innerT.default_for_goods_order_document = true)
                                     or (:defaultGoodsOrderEmail is not null and innerT.default_for_goods_order_email = true)
                                     or (:defaultLatePaymentFineDocument is not null and
                                         innerT.default_for_late_payment_fine_document = true)
                                     or
                                 (:defaultLatePaymentFineEmail is not null and innerT.default_for_late_payment_fine_email = true)
                                     or (coalesce(:defaultGoodsOrderDocument, :defaultGoodsOrderEmail,
                                                  :defaultLatePaymentFineDocument, :defaultLatePaymentFineEmail, '0') = '0')
                                 )
                               and (coalesce(:excludeVersions, '0') = '0' or (
                                 (:excludeVersions = 'OLDVERSION' and innerTD.start_date >=
                                                                      (select max(ctd.start_date)
                                                                       from template.template_details ctd
                                                                       where ctd.template_id = innerT.id
                                                                         and ctd.start_date <= current_date))
                                     or
                                 (:excludeVersions = 'FUTUREVERSION' and innerTD.start_date <=
                                                                         (select max(ctd.start_date)
                                                                          from template.template_details ctd
                                                                          where ctd.template_id = innerT.id
                                                                            and ctd.start_date <= current_date))
                                     or
                                 (:excludeVersions = 'OLDANDFUTUREVERSION' and innerTD.start_date =
                                                                               (select max(ctd.start_date)
                                                                                from template.template_details ctd
                                                                                where ctd.template_id = innerT.id
                                                                                  and ctd.start_date <= current_date))
                                 )
                                 )
                               and text(innerT.status) in (:statuses)
                               and (:prompt is null or (
                                 (:searchBy = 'ALL' and (
                                     (lower(text(innerT.template_purpose)) like :prompt)
                                         or (lower(innerTD.name) like :prompt)
                                         or (lower(text(innerTD.type)) like :prompt)
                                         or (lower(text(innerTD.file_signing)) like :prompt)
                                         or (lower(text(innerTD.output_file_format)) like :prompt)
                                         or (lower(text(innerT.create_date)) like :prompt)
                                         or (text(innerT.id) like :prompt)
                                     ))
                                     or (:searchBy = 'NAME' and lower(innerTD.name) like :prompt)
                                     or (:searchBy = 'ID' and text(innerT.id) like :prompt)
                                 )
                                 ))
            where td.start_date = (select max(innerTD.start_date)
                                   from template.template_details innerTD
                                   where innerTD.template_id = t.id
                                     and innerTD.start_date <= current_date)
                         """,
            countQuery = """
                    select count(t.id)
                    from template.templates t
                             join template.template_details td on t.id = td.template_id
                        and t.id in (select innerT.id
                                     from template.templates innerT
                                              join template.template_details innerTD on innerT.id = innerTD.template_id
                                     where ((:types) is null or text(innerTD.type) in (:types))
                                       and ((:outputFileFormats) is null or
                                            cast(innerTD.output_file_format as text[]) && cast(:outputFileFormats as text[]))
                                       and ((:templatePurposes) is null or text(innerT.template_purpose) in (:templatePurposes))
                                       and ((:fileSignings) is null or
                                            cast(innerTD.file_signing as text[]) && cast(:fileSignings as text[]))
                                       and ((:customerTypes) is null or
                                            cast(innerTD.customer_type as text[]) && cast(:customerTypes as text[]))
                                       and ((:consumptionPurposes) is null or
                                            cast(innerTD.purpose_of_consumption as text[]) && cast(:consumptionPurposes as text[]))
                                       and ((:languages) is null or text(innerTD.language) in (:languages))
                                       and (
                                         (:defaultGoodsOrderDocument is not null and innerT.default_for_goods_order_document = true)
                                             or (:defaultGoodsOrderEmail is not null and innerT.default_for_goods_order_email = true)
                                             or (:defaultLatePaymentFineDocument is not null and
                                                 innerT.default_for_late_payment_fine_document = true)
                                             or
                                         (:defaultLatePaymentFineEmail is not null and innerT.default_for_late_payment_fine_email = true)
                                             or (coalesce(:defaultGoodsOrderDocument, :defaultGoodsOrderEmail,
                                                          :defaultLatePaymentFineDocument, :defaultLatePaymentFineEmail, '0') = '0')
                                         )
                                       and (coalesce(:excludeVersions, '0') = '0' or (
                                         (:excludeVersions = 'OLDVERSION' and innerTD.start_date >=
                                                                              (select max(ctd.start_date)
                                                                               from template.template_details ctd
                                                                               where ctd.template_id = innerT.id
                                                                                 and ctd.start_date <= current_date))
                                             or
                                         (:excludeVersions = 'FUTUREVERSION' and innerTD.start_date <=
                                                                                 (select max(ctd.start_date)
                                                                                  from template.template_details ctd
                                                                                  where ctd.template_id = innerT.id
                                                                                    and ctd.start_date <= current_date))
                                             or
                                         (:excludeVersions = 'OLDANDFUTUREVERSION' and innerTD.start_date =
                                                                                       (select max(ctd.start_date)
                                                                                        from template.template_details ctd
                                                                                        where ctd.template_id = innerT.id
                                                                                          and ctd.start_date <= current_date))
                                         )
                                         )
                                       and text(innerT.status) in (:statuses)
                                       and (:prompt is null or (
                                         (:searchBy = 'ALL' and (
                                             (lower(text(innerT.template_purpose)) like :prompt)
                                                 or (lower(innerTD.name) like :prompt)
                                                 or (lower(text(innerTD.type)) like :prompt)
                                                 or (lower(text(innerTD.file_signing)) like :prompt)
                                                 or (lower(text(innerTD.output_file_format)) like :prompt)
                                                 or (lower(text(innerT.create_date)) like :prompt)
                                                 or (text(innerT.id) like :prompt)
                                             ))
                                             or (:searchBy = 'NAME' and lower(innerTD.name) like :prompt)
                                             or (:searchBy = 'ID' and text(innerT.id) like :prompt)
                                         )
                                         ))
                    where td.start_date = (select max(innerTD.start_date)
                                           from template.template_details innerTD
                                           where innerTD.template_id = t.id
                                             and innerTD.start_date <= current_date)
                                       """)
    Page<TemplateListingMiddleResponse> filter(@Param("prompt") String prompt,
                                               @Param("customerTypes") String customerTypes,
                                               @Param("consumptionPurposes") String consumptionPurposes,
                                               @Param("outputFileFormats") String outputFileFormats,
                                               @Param("templatePurposes") List<String> templatePurposes,
                                               @Param("fileSignings") String fileSignings,
                                               @Param("languages") List<String> languages,
                                               @Param("types") List<String> types,
                                               @Param("excludeVersions") String excludeVersionFromCheckBoxes,
                                               @Param("statuses") List<String> statuses,
                                               @Param("defaultGoodsOrderDocument") Boolean defaultGoodsOrderDocument,
                                               @Param("defaultGoodsOrderEmail") Boolean defaultGoodsOrderEmail,
                                               @Param("defaultLatePaymentFineDocument") Boolean defaultLatePaymentFineDocument,
                                               @Param("defaultLatePaymentFineEmail") Boolean defaultLatePaymentFineEmail,
                                               @Param("searchBy") String searchBy,
                                               Pageable pageable);


    boolean existsByIdAndTemplatePurposeAndStatus(Long templateId, ContractTemplatePurposes purposes, ContractTemplateStatus status);

    @Query("""
                            select  count (ctd.id)>0
                            from ContractTemplateDetail ctd
                        join ContractTemplate  ct on ct.id=ctd.templateId
                        where ct.status = 'ACTIVE'
                        and ct.id=:templateId
                        and ct.templatePurpose=:purpose
                        and ctd.templateType=:type
                        and ct.status='ACTIVE'
                        and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = :templateId and ctd2.startDate <=:currentDate)
            """)
    boolean existsByIdAndTemplatePurposeAndTemplateType(Long templateId,
                                                        ContractTemplatePurposes purpose,
                                                        ContractTemplateType type,
                                                        LocalDate currentDate);

    @Query("""
            select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,text(ctd.outputFileFormat),text(ctd.fileSigning))
                       from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate  ct on ct.id=ctd.templateId
            where (ct.status = 'ACTIVE' or ct.status='INACTIVE')
            and ct.id=:id
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = :id and ctd2.startDate <=:currentDate)
            """)
    Optional<ContractTemplateShortResponse> findTemplateResponseById(Long id, LocalDate currentDate);

    @Query("""
            select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(ctd.templateId,ctd.id,ctd.name,ctd.templateFileId,ctf.name,text(ctd.outputFileFormat),text(ctd.fileSigning))
                       from ContractTemplateDetail ctd
            join ContractTemplateFiles ctf on ctf.id=ctd.templateFileId
            join ContractTemplate  ct on ct.id=ctd.templateId
            where ct.status = 'ACTIVE'
            and ct.id=:id
            and ctd.startDate = (select max(ctd2.startDate) from ContractTemplateDetail ctd2 where ctd2.templateId = :id and ctd2.startDate <=:currentDate)
            """)
    Optional<ContractTemplateShortResponse> findTemplateResponseForCopy(Long id, LocalDate currentDate);

    @Query(value = """
            select new bg.energo.phoenix.model.response.template.ContractTemplateShortResponse(
                t.id,
                td.id,
                td.name,
                tf.id,
                tf.name,
                text(td.outputFileFormat),
                text(td.fileSigning)
            )
            from ContractTemplate t
                join ContractTemplateDetail td on td.templateId = t.id
                join ContractTemplateFiles tf on tf.id = td.templateFileId
            where td.id = :id
            """)
    Optional<ContractTemplateShortResponse> findInvoiceTemplateResponseByDetailId(@Param("id") Long id);

    @Query("""
            select td.id
            from BillingRun b
            left join ContractTemplate bt
                on (b.templateId = bt.id and bt.templatePurpose = 'INVOICE' and bt.status = 'ACTIVE')
                left join ServiceTemplate st
                                       on (:detailId = st.serviceDetailId and st.status = 'ACTIVE' and
                                           st.type = 'INVOICE_TEMPLATE')
                             left join ContractTemplate service_temp
                                       on (service_temp.id = st.templateId and service_temp.templatePurpose = 'INVOICE' and
                                           service_temp.status = 'ACTIVE')
                             left join ProductTemplate pt
                                       on (pt.productDetailId = :detailId and pt.status = 'ACTIVE' and
                                           pt.type = 'INVOICE_TEMPLATE')
                             left join ContractTemplate product_temp
                                       on (product_temp.id = pt.templateId and product_temp.templatePurpose = 'INVOICE' and
                                           product_temp.status = 'ACTIVE')
                             join ContractTemplate t on (t.id =
                                                           coalesce(bt.id, (case when :type = 'PRODUCT' then product_temp.id else service_temp.id end)))
                             join ContractTemplateDetail td on
                        (t.id = td.templateId and td.startDate = (select max(innerTD.startDate)
                                                                    from ContractTemplateDetail innerTD
                                                                    where innerTD.templateId = t.id
                                                                      and innerTD.startDate <= current_date))
                    where b.id = :id
            """)
    Optional<Long> findTemplateDetailForBilling(@Param("type") String type,
                                                @Param("detailId") Long detailId,
                                                @Param("id") Long id);

    @Query("""
            select t
            from ContractTemplate t
            where t.status = 'ACTIVE'
            and t.defaultForGoodsOrderDocument = true
            """)
    Optional<ContractTemplate> findDefaultForGoodsOrderDocument();

    @Query("""
            select t
            from ContractTemplate t
            where t.status = 'ACTIVE'
            and t.defaultForGoodsOrderEmail = true
            """)
    Optional<ContractTemplate> findDefaultForGoodsOrderEmail();

    @Query("""
            select t
            from ContractTemplate t
            where t.status = 'ACTIVE'
            and t.defaultForLatePaymentFineDocument = true
            """)
    Optional<ContractTemplate> findDefaultForLatePaymentFineDocument();

    @Query("""
            select t
            from ContractTemplate t
            where t.status = 'ACTIVE'
            and t.defaultForLatePaymentFineEmail = true
            """)
    Optional<ContractTemplate> findDefaultForLatePaymentFineEmail();

    @Query("""
           select ct
           from InvoiceCancellation ic
           join ContractTemplate ct on ic.emailTemplateId=ct.id
           where ct.status = "ACTIVE"
           and ic.id = :cancellationId
        """)
    Optional<ContractTemplate> findTemplateByInvoiceCancellationId(@Param("cancellationId") Long cancellationId);

    @Query("""
           select ct
           from LatePaymentFine lpf
           join ContractTemplate ct on lpf.templateId=ct.id
           where ct.status = "ACTIVE"
           and lpf.id = :latePaymentFineId
        """)
    Optional<ContractTemplate> findTemplateByLatePaymentFineId(@Param("latePaymentFineId") Long latePaymentFineId);

}
