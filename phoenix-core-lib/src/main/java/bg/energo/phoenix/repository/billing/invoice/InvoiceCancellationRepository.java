package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceCancellation;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.service.billing.invoice.models.persistance.extractor.InvoiceCancellationDocumentModelExtractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceCancellationRepository extends JpaRepository<InvoiceCancellation, Long> {
    @Query("""
            select ic
            from InvoiceCancellation ic
            join Process p on p.id = ic.processId
            join ProcessedRecordInfo pri on p.id = pri.processId
            where pri.id = :processRecordId
            """)
    Optional<InvoiceCancellation> findByProcessRecordInfo(Long processRecordId);

    Optional<InvoiceCancellation> findByProcessId(Long processId);

    @Query(value = """
            with invoice as (select inv.*
                             from invoice.invoices inv
                             where inv.id = :invoiceId),
                 customer as (select c.identifier                                        as customer_identifier,
                                     cd.vat_number                                       as vat_number,
                                     c.customer_number                                   as customer_number,
                                     (case
                                          when c.customer_type = 'PRIVATE_CUSTOMER'
                                              then concat(cd.name, ' ', cd.middle_name, ' ', cd.last_name)
                                          else concat(cd.name, ' ', lf.name) end)        as customer_name_comb,
                                     (case
                                          when c.customer_type = 'PRIVATE_CUSTOMER'
                                              then concat(cd.name_transl, ' ', cd.middle_name_transl, ' ', cd.last_name_transl)
                                          else concat(cd.name_transl, ' ', lf.name) end) as customer_name_comb_trsl,
                                     case
                                         when cd.foreign_address = false then
                                             concat_ws(', ',
                                                       nullif(distr.name, ''),
                                                       nullif(concat_ws(' ',
                                                                        replace(text(cd.residential_area_type), '_', ' '),
                                                                        ra.name), ''),
                                                       nullif(
                                                               concat_ws(' ', cd.street_type, str.name, cd.street_number),
                                                               ''),
                                                       nullif(concat('бл. ', cd.block), 'бл. '),
                                                       nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                       nullif(concat('ет. ', cd.floor), 'ет. '),
                                                       nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                       cd.address_additional_info
                                             )
                                         else
                                             concat_ws(', ',
                                                       nullif(cd.district_foreign, ''),
                                                       nullif(concat_ws(' ',
                                                                        replace(text(cd.foreign_residential_area_type), '_', ' '),
                                                                        cd.residential_area_foreign), ''),
                                                       nullif(
                                                               concat_ws(' ', cd.foreign_street_type, cd.street_foreign,
                                                                         cd.street_number),
                                                               ''),
                                                       nullif(concat('бл. ', cd.block), 'бл. '),
                                                       nullif(concat('вх. ', cd.entrance), 'вх. '),
                                                       nullif(concat('ет. ', cd.floor), 'ет. '),
                                                       nullif(concat('ап. ', cd.apartment), 'ап. '),
                                                       cd.address_additional_info
                                             )
                                         end                                             as customer_addr_comb,
                                     case
                                         when cd.foreign_address = false then pp.name
                                         else cd.populated_place_foreign end             as populated_place,
                                     case
                                         when cd.foreign_address = false then zc.zip_code
                                         else cd.zip_code_foreign end                    as zip_code,
                                     case
                                         when cd.foreign_address = false then distr.name
                                         else cd.district_foreign end                    as district,
                                     case
                                         when cd.foreign_address = false then replace(text(ra.type), '_', ' ')
                                         else replace(text(cd.residential_area_type), '_', ' ') end       as quarter_ra_type,
                                     case
                                         when cd.foreign_address = false then ra.name
                                         else cd.residential_area_foreign end            as quarter_ra_name,
                                     case
                                         when cd.foreign_address = false then cd.street_type
                                         else cd.foreign_street_type end                 as blvd_str_type,
                                     case
                                         when cd.foreign_address = false then str.name
                                         else cd.street_foreign end                      as str_blvd_name,
                                     cd.street_number                                    as str_blvd_number,
                                     cd.block                                            as block,
                                     cd.entrance                                         as entrance,
                                     cd.floor                                            as floor,
                                     cd.apartment                                        as apartment,
                                     cd.address_additional_info                          as additional_info
                              from invoice inv
                                       join customer.customer_details cd on cd.id = inv.customer_detail_id
                                       join customer.customers c on cd.customer_id = c.id
                                       left join nomenclature.legal_forms lf on cd.legal_form_id = lf.id
                                       left join nomenclature.districts distr on cd.district_id = distr.id
                                       left join nomenclature.zip_codes zc on cd.zip_code_id = zc.id
                                       left join nomenclature.residential_areas ra on cd.residential_area_id = ra.id
                                       left join nomenclature.streets str on cd.street_id = str.id
                                       left join nomenclature.populated_places pp on cd.populated_place_id = pp.id),
                 customer_segments as (select s.name as segment
                                       from invoice inv
                                                join customer.customer_details cd on cd.id = inv.customer_detail_id
                                                join customer.customer_segments cs on cd.id = cs.customer_detail_id
                                                join nomenclature.segments s on cs.segment_id = s.id),
                 customer_managers as (SELECT JSON_AGG(
                                                      JSON_BUILD_OBJECT(
                                                              'title', title.name, -- Join the titles table directly
                                                              'name', cm.name,
                                                              'surname', cm.surname,
                                                              'jobPosition', cm.job_position
                                                      )
                                              ) AS managers
                                       from invoice inv
                                                join customer.customer_managers cm on cm.customer_detail_id = inv.customer_detail_id
                                                LEFT JOIN nomenclature.titles title ON title.id = cm.title_id
                                       GROUP BY cm.customer_detail_id)
            select (select customer_name_comb from customer)                as CustomerNameComb,
                   (select customer_name_comb_trsl from customer)           as CustomerNameCombTrsl,
                   (select customer_identifier from customer)               as CustomerIdentifer,
                   (select vat_number from customer)                        as CustomerVat,
                   (select customer_number from customer)                   as CustomerNumber,
                   translation.translate_text((select customer_addr_comb from customer),text('BULGARIAN'))                as CustomerAddressComb,
                   (select populated_place from customer)                   as CustomerPopulatedPlace,
                   (select zip_code from customer)                          as CustomerZip,
                   (select district from customer)                          as CustomerDistrict,
                   translation.translate_text((select quarter_ra_type from customer) ,text('BULGARIAN'))                  as CustomerQuarterRaType,
                   (select quarter_ra_name from customer)                   as CustomerQuarterRaName,
                   translation.translate_text(text((select blvd_str_type from customer)),text('BULGARIAN'))                     as CustomerStrBlvdType,
                   (select str_blvd_name from customer)                     as CustomerStrBlvdName,
                   (select str_blvd_number from customer)                   as CustomerStrBlvdNumber,
                   (select block from customer)                             as CustomerBlock,
                   (select entrance from customer)                          as CustomerEntrance,
                   (select floor from customer)                             as CustomerFloor,
                   (select apartment from customer)                         as CustomerApartment,
                   (select additional_info from customer)                   as CustomerAdditionalInfo,
                   (select array_agg(segment) from customer_segments cs)    as CustomerSegments,
                   (select managers from customer_managers)                 as Managers,
                   substring(inv.invoice_cancellation_number from '-(.*)')  as DocumentNumber,
                   substring(inv.invoice_cancellation_number from '^[^-]+') as DocumentPrefix,
                   inv.invoice_date                                         as DocumentDate,
                   inv.document_type                                        as CanceledDocumentType,
                   substring(inv.invoice_number from '-(.*)')               as CanceledDocumentNumber,
                   substring(inv.invoice_number from '^[^-]+')              as CanceledDocumentPrefix
            from invoice inv
            """, nativeQuery = true)
    InvoiceCancellationDocumentModelExtractor getDocumentModelForCancelledInvoice(Long invoiceId);

    @Query("""
            select doc from InvoiceCancellation ic 
            join Invoice inv on inv.invoiceCancellationId=ic.id
            join InvoiceCancellationDocument idc on idc.cancelledInvoiceId=inv.id
            join Document doc on doc.id=idc.documentId   
            where ic.processId=:processId
            and text(doc.documentStatus)='UNSIGNED'
            """)
    List<Document> findInvoiceCancellationDocumentsByProcessId(Long processId);


    @Query(value = """
            select iinv
            from Invoice minv
                     join BillingRun b on minv.billingId = b.id
                join BillingRunInvoices bi on bi.billingId=b.id
                     join Invoice iinv on iinv.id = bi.invoiceId and iinv.invoiceType='INTERIM_AND_ADVANCE_PAYMENT'
            where minv.id = :invoiceId
            and not exists(select 1 from Invoice stInv
                                    where stInv.id=iinv.standardInvoiceId
                                      and stInv.invoiceStatus<>'CANCELLED'
                                    union all select 1 from BillingRunInvoices bi2
                                                       join Invoice genInv on genInv.billingId=bi2.billingId and genInv.invoiceStatus='REAL'
                                                       where bi2.invoiceId=iinv.id and bi2.billingId <> b.id)
            """)
    List<Invoice> findDeductedInvoicesForManual(Long invoiceId);

    @Query(value = """
            select minv from Invoice minv
            where minv.standardInvoiceId=:invoiceId
            and not exists(select 1 from BillingRunInvoices bi2
                                             join Invoice genInv on genInv.billingId=bi2.billingId and genInv.invoiceStatus='REAL'
                           where bi2.billingId=minv.id)
            and minv.invoiceStatus='REAL'
            """)
    List<Invoice> findDeductedForStandard(Long invoiceId);
}
