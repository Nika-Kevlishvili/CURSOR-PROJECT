package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentDetailedDataDAO;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentSummeryDataDAO;
import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentDetailedDataProjection;
import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentSummeryDataProjection;
import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentVatBaseProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceDocumentDataRepository extends JpaRepository<Invoice, Long> {
    // summary Data

    // todo -> measure_unit_of_price_in_other_currency
    @Query(value = """
            select 'MANUAL'                                                         as detail_type,
                   sd.price_component_or_price_component_groups                     as price_component,
                   sd.total_volumes                                                 as total_volumes,
                   sd.measures_unit_for_total_volumes                               as measure_unit_for_total_volumes,
                   sd.unit_price                                                    as price,
                   cast(sd.value_currency_exchange_rate * sd.unit_price as decimal) as price_other_currency,
                   sd.measure_for_unit_price                                        as measure_unit_of_price,
                   sd.value                                                         as value,
                   sd.value_currency_name                                           as measure_unit_of_value,
                   'DIRECT'                                                         as price_component_connection_type,
                   sd.vat_rate_percent                                              as vat_rate_percent
            from invoice.manual_invoice_summary_data sd
            where sd.invoice_id = :invoiceId
            """, nativeQuery = true)
    List<BillingRunDocumentSummeryDataProjection> getManualInvoiceSummaryData(Long invoiceId);

    // todo -> measure_unit_of_price_in_other_currency
    @Query(value = """
            select 'MANUAL'                                                         as detail_type,
                   sd.price_component_or_price_component_groups                     as price_component,
                   sd.total_volumes                                                 as total_volumes,
                   sd.measures_unit_for_total_volumes                               as measure_unit_for_total_volumes,
                   sd.unit_price                                                    as price,
                   cast(sd.value_currency_exchange_rate * sd.unit_price as decimal) as price_other_currency,
                   sd.measure_for_unit_price                                        as measure_unit_of_price,
                   sd.value                                                         as value,
                   sd.value_currency_name                                           as measure_unit_of_value,
                   'DIRECT'                                                         as price_component_connection_type,
                   sd.vat_rate_percent                                              as vat_rate_percent
            from invoice.manual_debit_or_credit_note_invoice_summary_data sd
            where sd.invoice_id = :invoiceId
            """, nativeQuery = true)
    List<BillingRunDocumentSummeryDataProjection> getManualDebitOrCreditNoteSummaryData(Long invoiceId);

    // todo -> vat_rate_name
    @Query(value = """
            select 'MANUAL_INTERIM_ADVANCE_PAYMENT'     as detail_type,
                   'Manual interim and advance payment' as price_component,
                   null                                 as total_volumes,
                   null                                 as measure_unit_for_total_volumes,
                   null                                 as price,
                   null                                 as price_other_currency,
                   null                                 as measure_unit_of_price,
                   inv.total_amount_excluding_vat       as value,
                   mc.name                              as measure_unit_of_value,
                   'DIRECT'                             as price_component_connection_type,
                   vr.vat_rate_percent                  as vat_rate_percent
            from invoice.invoices inv
                     left join nomenclature.currencies mc on mc.id = inv.currency_id
                     left join invoice.invoice_vat_rate_values vr on vr.invoice_id = inv.id
            where inv.id = :invoiceId
            """, nativeQuery = true)
    List<BillingRunDocumentSummeryDataProjection> getManualInterimAdvancePaymentSummaryData(Long invoiceId);

    @Query(value = """
            select 'INTERIM_ADVANCE_PAYMENT'                as detail_type,
                   (select iap.name
                    from interim_advance_payment.interim_advance_payments iap
                    where iap.id = d.interim_id)            as price_component,
                   null                                     as total_volumes,
                   null                                     as measure_unit_for_total_volumes,
                   null                                     as price,
                   null                                     as price_other_currency,
                   null                                     as measure_unit_of_price,
                   d.main_currency_total_amount_without_vat as value,
                   (select c.name
                    from nomenclature.currencies c
                    where c.id = d.original_currency_id)    as measure_unit_of_value,
                   'DIRECT'                                 as price_component_connection_type,
                   d.vat_rate_percent                       as vat_rate_percent
            from invoice.invoice_standard_detailed_data d
            where d.invoice_id = :invoiceId
            """, nativeQuery = true)
    List<BillingRunDocumentSummeryDataProjection> getStandardInterimAdvancePaymentSummaryData(Long invoiceId);

    // todo -> measure_unit_of_price_in_other_currency, measure_unit_of_value, price_other_currency, price_component
    @Query(value = """
            select summary_data.detail_type                     as detail_type,
                   summary_data.price_component                 as price_component,
                   summary_data.total_volumes                   as total_volumes,
                   summary_data.measure_unit_for_total_volumes  as measure_unit_for_total_volumes,
                   summary_data.price                           as price,
                   summary_data.price_other_currency            as price_other_currency,
                   summary_data.measure_unit_of_price           as measure_unit_of_price,
                   summary_data.value                           as value,
                   summary_data.measure_unit_of_value           as measure_unit_of_value,
                   summary_data.vat_rate_percent                as vat_rate_percent,
                   summary_data.price_component_connection_type as price_component_connection_type
            from (select d.detail_type                                                       as detail_type,
                         pc.name                                                             as price_component,
                         case when d.detail_type <> 'DISCOUNT' then sum(d.total_volumes) end as total_volumes,
                         case
                             when d.detail_type = 'PER_PIECE' then
                                 (select su.name from nomenclature.service_units su where d.measures_unit_for_total_volumes = su.id)
                             else (case
                                       when (d.detail_type = 'SCALE' or d.detail_type = 'SETTLEMENT')
                                           then 'Kwh' end) end                               as measure_unit_for_total_volumes,
                         case
                             when text(d.detail_type) in ('SETTLEMENT', 'SCALE', 'PER_PIECE') then
                                 avg(d.unit_price) end
                                                                                             as price,
                         cast(d.unit_price * c.alt_ccy_exchange_rate as decimal)             as price_other_currency,
                         case
                             when (d.detail_type = 'SCALE' or d.detail_type = 'SETTLEMENT' or d.detail_type = 'PER_PIECE')
                                 then (select pcvt.name
                                       from nomenclature.price_component_value_types pcvt
                                       where pc.price_component_value_type_id = pcvt.id)
                             else case when (d.detail_type = 'DISCOUNT') then c.name end end as measure_unit_of_price,
                         sum(d.main_currency_total_amount_without_vat)                       as value,
                         c.name                                                              as measure_unit_of_value,
                         d.vat_rate_percent                                                  as vat_rate_percent,
                         d.pc_group_detail_id                                                as group_detail_id,
                         case
                             when d.pc_group_detail_id is null then 'DIRECT'
                             else 'FROM_PC_GROUP' end                                        as price_component_connection_type,
                         pc.id                                                               as pc_id
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           join nomenclature.currencies c on c.id = d.main_currency_id
                  where d.invoice_id = :invoiceId
                  group by d.unit_price, pc.name, d.income_account_number, d.cost_center_controlling_order,
                           d.vat_rate_percent, d.pc_group_detail_id, pc.id, d.detail_type, d.measures_unit_for_total_volumes,
                           d.main_currency_id, c.name, c.alt_ccy_exchange_rate,pc.don_not_include_in_the_vat_base
                  union
                  select d.detail_type                                           as detail_type,
                         iap.name                                                as price_component,
                         null                                                    as total_volumes,
                         null                                                    as measure_unit_for_total_volumes,
                         null                                                    as price,
                         cast(d.unit_price * c.alt_ccy_exchange_rate as decimal) as price_other_currency,
                         null                                                    as measure_unit_of_price,
                         d.main_currency_total_amount_without_vat                as value,
                         c.name                                                  as measure_unit_of_value,
                         d.vat_rate_percent                                      as vat_rate_percent,
                         null                                                    as group_detail_id,
                         'DIRECT'                                                as price_component_connection_type,
                         d.interim_id                                            as pc_id
                  from invoice.invoice_standard_detailed_data d
                           join interim_advance_payment.interim_advance_payments iap on d.interim_id = iap.id
                           join nomenclature.currencies c on c.id = d.main_currency_id
                  where d.invoice_id = :invoiceId
                    and d.detail_type = 'INTERIM_DEDUCTION'
                  union
                  select d.detail_type                                 as detail_type,
                         pcgd.name                                     as price_component,
                         null                                          as total_volumes,
                         null                                          as measure_unit_for_total_volumes,
                         null                                          as price,
                         null                                          as price_other_currency,
                         null                                          as measure_unit_of_price,
                         sum(d.main_currency_total_amount_without_vat) as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.main_currency_id)             as measure_unit_of_value,
                         d.vat_rate_percent                            as vat_rate_percent,
                         d.pc_group_detail_id                          as group_detail_id,
                         'GROUP'                                       as price_component_connection_type,
                         null
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           join nomenclature.currencies c on c.id = d.main_currency_id
                           join price_component.price_component_group_details pcgd on d.pc_group_detail_id = pcgd.id
                  where d.invoice_id = :invoiceId
                  group by d.unit_price, d.detail_type, pcgd.name, measure_unit_of_value, d.pc_group_detail_id,
                           c.alt_ccy_exchange_rate, d.vat_rate_percent) as summary_data
            order by case
                         when group_detail_id is null then 1
                         else 2
                         end,
                     group_detail_id,
                     case
                         when price_component_connection_type = 'GROUP' then 1
                         else 2
                         end,
                     pc_id
            """, nativeQuery = true)
    List<BillingRunDocumentSummeryDataProjection> getStandardInvoiceSummaryData(Long invoiceId);

    @Query(value = """
            select 'GOODS_ORDER'                                              as detail_type,
                   idd.good_name                                              as price_component,
                   idd.total_volumes                                          as total_volumes,
                   gu.name                                                    as measure_unit_for_total_volumes,
                   idd.unit_price                                             as price,
                   cast(idd.unit_price * mc.alt_ccy_exchange_rate as decimal) as price_other_currency,
                   mc.name                                                    as measure_unit_of_price,
                   idd.value                                                  as value,
                   mc.name                                                    as measure_unit_of_value,
                   'DIRECT'                                                   as price_component_connection_type,
                   idd.vat_rate_percent                                       as vat_rate_percent
            from invoice.invoice_detailed_data idd
                     left join nomenclature.goods_units gu on gu.id = idd.measure_unit_for_total_volumes_go
                     left join nomenclature.currencies mc on mc.id = idd.measure_unit_for_value_go
            where idd.invoice_id = :invoiceId
            
            """, nativeQuery = true)
    List<BillingRunDocumentSummeryDataProjection> getGoodsOrderSummaryData(Long invoiceId);

    @Query(value = """
            select 'SERVICE_ORDER'                                                                   as detail_type,
                   pc.name                                                                           as price_component,
                   idd.total_volumes                                                                 as total_volumes,
                   su.name                                                                           as measure_unit_for_total_volumes,
                   idd.unit_price                                                                    as price,
                   cast(idd.unit_price * mc.alt_ccy_exchange_rate as decimal)                        as price_other_currency,
                   mc.name                                                                           as measure_unit_of_price,
                   idd.value                                                                         as value,
                   mc.name                                                                           as measure_unit_of_value,
                   (case when idd.pc_group_detail_id is null then 'DIRECT' else 'FROM_PC_GROUP' end) as price_component_connection_type,
                   idd.vat_rate_percent                                                         as vat_rate_percent
            from invoice.invoices inv
                     join invoice.invoice_detailed_data idd on idd.invoice_id = inv.id
                     left join service_order.orders so on so.id = inv.service_order_id
                     left join price_component.price_components pc on idd.price_component_id = pc.id
                     left join nomenclature.service_units su on su.id = idd.measure_unit_for_total_volumes_so
                     left join nomenclature.currencies mc on mc.id = idd.measure_unit_for_value_go
            where inv.id = :invoiceId
            union
            select distinct 'SERVICE_ORDER'                   as detail_type,
                            pcgd.name                         as price_component,
                            0                                 as total_volumes,
                            null                              as measure_unit_for_total_volumes,
                            0                                 as price,
                            0                                 as price_other_currency,
                            null                              as measure_unit_of_price,
                            (select sum(inidd.value)
                             from invoice.invoice_detailed_data inidd
                             where inidd.pc_group_detail_id = idd2.pc_group_detail_id
                               and inidd.invoice_id = inv.id) as value,
                            mc.name                           as measure_unit_of_value,
                            'GROUP'                           as price_component_connection_type,
                            0                                 as vat_rate_percent
            from invoice.invoices inv
                     join invoice.invoice_detailed_data idd2 on idd2.invoice_id = inv.id
                     left join service_order.orders so on so.id = inv.service_order_id
                     join price_component.price_component_group_details pcgd on pcgd.id = idd2.pc_group_detail_id
                     left join nomenclature.currencies mc on mc.id = idd2.measure_unit_for_value_go
            where inv.id = :invoiceId
           """, nativeQuery = true)
    List<BillingRunDocumentSummeryDataProjection> getServiceOrderSummaryData(Long invoiceId);

    // detailed data

    @Query(value = """
            select dd.detail_type                                                        as detail_type,
                   pod.identifier                                                        as pod,
                   pod_detail.additional_identifier                                      as pod_additional_identifier,
                   pod_detail.name                                                       as pod_name,
                   pod_detail.measurement_type                                           as meteringType,
                   mt.name                                                               as measurementType,
                   case
                       when pod_detail.foreign_address = false then
                           concat_ws(', ',
                                     nullif(distr.name, ''),
                                     nullif(concat_ws(' ',
                                                      case when ra.name is null then null else replace(text(ra.type), '_', ' ') end,
                                                      ra.name), ''),
                                     nullif(concat_ws(' ', case when str.name is null then null else str.type end, str.name,
                                                      case when str.name is null then null else pod_detail.street_number end), ''),
                                     nullif(concat('бл. ', pod_detail.block), 'бл. '),
                                     nullif(concat('вх. ', pod_detail.entrance), 'вх. '),
                                     nullif(concat('ет. ', pod_detail.floor), 'ет. '),
                                     nullif(concat('ап. ', pod_detail.apartment), 'ап. '),
                                     pod_detail.address_additional_info
                           )
                       else
                           concat_ws(', ',
                                     nullif(pod_detail.district_foreign, ''),
                                     nullif(concat_ws(' ',
                                                      case
                                                          when pod_detail.residential_area_foreign is null then null
                                                          else replace(text(pod_detail.foreign_residential_area_type), '_', ' ') end,
                                                      pod_detail.residential_area_foreign), ''),
                                     nullif(
                                             concat_ws(' ', case
                                                                when pod_detail.street_foreign is null then null
                                                                else pod_detail.foreign_street_type end, pod_detail.street_foreign,
                                                       case
                                                           when pod_detail.street_foreign is null then null
                                                           else pod_detail.street_number end),
                                             ''),
                                     nullif(concat('бл. ', pod_detail.block), 'бл. '),
                                     nullif(concat('вх. ', pod_detail.entrance), 'вх. '),
                                     nullif(concat('ет. ', pod_detail.floor), 'ет. '),
                                     nullif(concat('ап. ', pod_detail.apartment), 'ап. '),
                                     pod_detail.address_additional_info
                           )
                       end                                                               as pod_address_comb,
                   case
                       when pod_detail.foreign_address
                           then pod_detail.populated_place_foreign
                       else (select pp.name from nomenclature.populated_places pp where pp.id = pod_detail.populated_place_id)
                       end                                                               as pod_place,
                   case
                       when pod_detail.foreign_address
                           then pod_detail.zip_code_foreign
                       else (select zc.zip_code from nomenclature.zip_codes zc where zc.id = pod_detail.zip_code_id)
                       end                                                               as pod_zip,
                   dd.date_from                                                          as period_from,
                   dd.date_to                                                            as period_to,
                   m.number                                                              as meter,
                   pc.name                                                               as price_component,
                   pc.don_not_include_in_the_vat_base                                    as doNotIncludeVatBase,
                   dd.main_currency_total_amount_without_vat                             as value,
                   mc.name                                                               as measure_unit_of_value,
                   dd.total_volumes                                                      as total_volumes,
                   case
                       when dd.detail_type = 'PER_PIECE'
                           then
                           (select su.name
                            from nomenclature.service_units su
                            where dd.measures_unit_for_total_volumes = su.id)
                       else (case
                                 when (dd.detail_type = 'SCALE' or dd.detail_type = 'SETTLEMENT')
                                     then 'Kwh' end) end                                 as measure_unit_for_total_volumes,
                   dd.unit_price                                                         as price,
                   cast(dd.unit_price * mc.alt_ccy_exchange_rate as decimal)             as price_in_other_currency,
                   case
                       when (dd.detail_type = 'SCALE' or dd.detail_type = 'SETTLEMENT' or dd.detail_type = 'PER_PIECE')
                           then (select pcvt.name
                                 from nomenclature.price_component_value_types pcvt
                                 where pc.price_component_value_type_id = pcvt.id)
                       else case when (dd.detail_type = 'DISCOUNT') then mc.name end end as measure_unit_of_price,
                   dd.vat_rate_percent                                                   as vat_rate_percent,
                   dd.new_meter_reading                                                  as new_meter_reading,
                   dd.old_meter_reading                                                  as old_meter_reading,
                   case
                       when inv.product_detail_id is not null
                           then (select pd.printing_name from product.product_details pd where pd.id = inv.product_detail_id)
                       when inv.service_detail_id is not null
                           then (select sd.printing_name from service.service_details sd where sd.id = inv.service_detail_id)
                       when inv.goods_order_id is not null
                           then (select go.order_number from goods_order.orders go where go.id = inv.goods_order_id)
                       when inv.service_order_id is not null
                           then (select sd.printing_name
                                 from service_order.orders so
                                          left join service.service_details sd on sd.id = so.service_detail_id
                                 where so.id = inv.service_order_id)
                       end                                                               as product_name,
                   dd.difference                                                         as difference,
                   dd.multiplier                                                         as multiplier,
                   dd.correction                                                         as correction,
                   dd.deducted                                                           as deducted,
                   coalesce(sc.scale_code, sc.tariff_scale)                              as scaleCode
            from invoice.invoices inv
                     left join invoice.invoice_standard_detailed_data dd on dd.invoice_id = inv.id
                     left join pod.pod pod on pod.id = dd.pod_id
                     left join pod.pod_details pod_detail on pod_detail.id = dd.pod_detail_id
                     left join pod.meters m on m.id = dd.meter_id
                     left join price_component.price_components pc on pc.id = dd.pc_id
                     left join price_component.price_component_group_details pcgd on pcgd.id = dd.pc_group_detail_id
                     left join nomenclature.currencies mc on mc.id = dd.main_currency_id
                     left join nomenclature.districts distr on distr.id = pod_detail.district_id
                     left join nomenclature.residential_areas ra on ra.id = pod_detail.residential_area_id
                     left join nomenclature.streets str on str.id = pod_detail.street_id
                     left join nomenclature.pod_measurement_types mt on mt.id = pod_detail.pod_measurement_types_id
                     left join nomenclature.scales sc on sc.id = dd.scale_id
            where inv.id = :invoiceId
              and detail_type in ('SCALE', 'SETTLEMENT')
            union
            select dd.detail_type                                                                        as detail_type,
                   coalesce(pod.identifier, dd.unrecognized_pod)                                         as pod,
                   pod_detail.additional_identifier                                                      as pod_additional_identifier,
                   pod_detail.name                                                                       as pod_name,
                   pod_detail.measurement_type                                                           as meteringType,
                   mt.name                                                                               as measurementType,
                   case
                       when pod_detail.foreign_address = false then
                           concat_ws(', ',
                                     nullif(distr.name, ''),
                                     nullif(concat_ws(' ', replace(text(ra.type), '_', ' '), ra.name), ''),
                                     nullif(concat_ws(' ', str.type, str.name, pod_detail.street_number), ''),
                                     nullif(concat('бл. ', pod_detail.block), 'бл. '),
                                     nullif(concat('вх. ', pod_detail.entrance), 'вх. '),
                                     nullif(concat('ет. ', pod_detail.floor), 'ет. '),
                                     nullif(concat('ап. ', pod_detail.apartment), 'ап. '),
                                     pod_detail.address_additional_info
                           )
                       else
                           concat_ws(', ',
                                     nullif(pod_detail.district_foreign, ''),
                                     nullif(concat_ws(' ',
                                                      replace(text(pod_detail.foreign_residential_area_type), '_', ' '),
                                                      pod_detail.residential_area_foreign), ''),
                                     nullif(
                                             concat_ws(' ', pod_detail.foreign_street_type, pod_detail.street_foreign,
                                                       pod_detail.street_number),
                                             ''),
                                     nullif(concat('бл. ', pod_detail.block), 'бл. '),
                                     nullif(concat('вх. ', pod_detail.entrance), 'вх. '),
                                     nullif(concat('ет. ', pod_detail.floor), 'ет. '),
                                     nullif(concat('ап. ', pod_detail.apartment), 'ап. '),
                                     pod_detail.address_additional_info
                           )
                       end                                                                               as pod_address_comb,
                   case
                       when pod_detail.foreign_address
                           then pod_detail.populated_place_foreign
                       else (select pp.name from nomenclature.populated_places pp where pp.id = pod_detail.populated_place_id)
                       end                                                                               as pod_place,
                   case
                       when pod_detail.foreign_address
                           then pod_detail.zip_code_foreign
                       else (select zc.zip_code from nomenclature.zip_codes zc where zc.id = pod_detail.zip_code_id)
                       end                                                                               as pod_zip,
                   dd.date_from                                                                          as period_from,
                   dd.date_to                                                                            as period_to,
                   m.number                                                                              as meter,
                   pc.name                                                                               as price_component,
                   pc.don_not_include_in_the_vat_base                                                    as doNotIncludeVatBase,
                   dd.main_currency_total_amount_without_vat                                             as value,
                   mc.name                                                                               as measure_unit_of_value,
                   dd.total_volumes                                                                      as total_volumes,
                   'бр.'                                                                                 as measure_unit_for_total_volumes,
                   dd.main_currency_total_amount_without_vat                                             as price,
                   cast(dd.main_currency_total_amount_without_vat * mc.alt_ccy_exchange_rate as decimal) as price_in_other_currency,
                   pcvt.name                                                                             as measure_unit_of_price,
                   dd.vat_rate_percent                                                                   as vat_rate_percent,
                   dd.new_meter_reading                                                                  as new_meter_reading,
                   dd.old_meter_reading                                                                  as old_meter_reading,
                   case
                       when inv.product_detail_id is not null
                           then (select pd.printing_name from product.product_details pd where pd.id = inv.product_detail_id)
                       when inv.service_detail_id is not null
                           then (select sd.printing_name from service.service_details sd where sd.id = inv.service_detail_id)
                       when inv.goods_order_id is not null
                           then (select go.order_number from goods_order.orders go where go.id = inv.goods_order_id)
                       when inv.service_order_id is not null
                           then (select sd.printing_name
                                 from service_order.orders so
                                          left join service.service_details sd on sd.id = so.service_detail_id
                                 where so.id = inv.service_order_id)
                       end                                                                               as product_name,
                   dd.difference                                                                         as difference,
                   dd.multiplier                                                                         as multiplier,
                   dd.correction                                                                         as correction,
                   dd.deducted                                                                           as deducted,
                   coalesce(sc.scale_code, sc.tariff_scale)                                              as scaleCode
            from invoice.invoices inv
                     left join invoice.invoice_standard_detailed_data dd on dd.invoice_id = inv.id
                     left join pod.pod pod on pod.id = dd.pod_id
                     left join pod.pod_details pod_detail on (pod_detail.id = coalesce(dd.pod_detail_id, pod.last_pod_detail_id))
                     left join pod.meters m on m.id = dd.meter_id
                     left join price_component.price_components pc on pc.id = dd.pc_id
                     left join price_component.price_component_group_details pcgd on pcgd.id = dd.pc_group_detail_id
                     left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                     left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                     left join nomenclature.currencies mc on mc.id = dd.main_currency_id
                     left join nomenclature.districts distr on distr.id = pod_detail.district_id
                     left join nomenclature.residential_areas ra on ra.id = pod_detail.residential_area_id
                     left join nomenclature.streets str on str.id = pod_detail.street_id
                     left join nomenclature.pod_measurement_types mt on mt.id = pod_detail.pod_measurement_types_id
                     left join nomenclature.scales sc on sc.id = dd.scale_id
            where inv.id = :invoiceId
              and text(dd.detail_type) in
                  ('OVER_TIME_ONE_TIME', 'OVER_TIME_PERIODICAL', 'WITH_ELECTRICITY')
              and (dd.pod_id is not null or dd.unrecognized_pod is not null)
            """, nativeQuery = true)
    List<BillingRunDocumentDetailedDataProjection> getStandardInvoiceDetailedData(Long invoiceId);
    @Query("""
            select new bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentDetailedDataDAO(
                midd.priceComponentOrPriceComponentGroups,
                midd.value,
                midd.valueCurrencyName,
                midd.totalVolumes,
                midd.measuresUnitForTotalVolumes,
                midd.unitPrice,
                cast((midd.unitPrice * i.currencyExchangeRateOnInvoiceCreation) as bigdecimal),
                midd.measureForUnitPrice,
                midd.vatRatePercent,
                midd.newMeterReading,
                midd.oldMeterReading,
                midd.differences,
                midd.correction,
                midd.deducted,
                midd.multiplier,
                midd.periodFrom,
                midd.periodTo
            )
            from ManualInvoiceDetailedData midd
            join Invoice i on i.id = midd.invoiceId
            where i.id = :id
            """)
    List<BillingRunDocumentDetailedDataDAO> getManualInvoiceDetailedData(Long id);

    @Query("""
            select new bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentDetailedDataDAO(
                midd.priceComponentOrPriceComponentGroups,
                midd.value,
                midd.valueCurrencyName,
                midd.totalVolumes,
                midd.measuresUnitForTotalVolumes,
                midd.unitPrice,
                cast((midd.unitPrice * i.currencyExchangeRateOnInvoiceCreation) as bigdecimal),
                midd.measureForUnitPrice,
                midd.vatRatePercent,
                midd.newMeterReading,
                midd.oldMeterReading,
                midd.differences,
                midd.correction,
                midd.deducted,
                midd.multiplier,
                midd.periodFrom, 
                midd.periodTo
            )
            from ManualDebitOrCreditNoteInvoiceDetailedData midd
            join Invoice i on i.id = midd.invoiceId
            where i.id = :id
            """)
    List<BillingRunDocumentDetailedDataDAO> getManualDebitCreditNoteInvoiceDetailedData(Long id);

    @Query("""
                    select new bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentSummeryDataDAO(
                        pc.name,
                        sum(idd.totalVolumes),
                        su.name,
                        idd.unitPrice,
                        cast((idd.unitPrice * i.currencyExchangeRateOnInvoiceCreation) as bigdecimal),
                        sum(idd.value),
                        mc.name,
                        idd.vatRatePercent,
                        pcgd.name
                    )
                    from InvoiceDetailedData idd
                    join Invoice i on i.id = idd.invoiceId
                    left join PriceComponent pc on idd.priceComponentId = pc.id
                    left join PriceComponentGroupDetails pcgd on pcgd.id = idd.pcGroupDetailId
                    left join ServiceUnit su on su.id = idd.measureUnitForTotalVolumesServiceOrder
                    left join Currency mc on mc.id = idd.measureUnitForValueOrders
                    left join VatRate v on v.id = idd.vatRateId
                    where idd.invoiceId = :invoiceId
                    group by pc.name, su.name, idd.unitPrice, i.currencyExchangeRateOnInvoiceCreation,
                    mc.name, idd.vatRatePercent, pcgd.name
            """)
    List<BillingRunDocumentSummeryDataDAO> getServiceOrderSummaryDataForDocument(Long invoiceId);

    @Query(nativeQuery = true, value = """
            select 'SERVICE_ORDER'                                           as detail_type,
                   coalesce(pod.identifier, dd.unrecognized_pod)             as pod,
                   pod_detail.additional_identifier                          as pod_additional_identifier,
                   pod_detail.name                                           as pod_name,
                   pod_detail.measurement_type                               as meteringType,
                   mt.name                                                   as measurmentType,
                   case
                       when pod_detail.foreign_address = false then
                           concat_ws(', ',
                                     nullif(distr.name, ''),
                                     nullif(concat_ws(' ', replace(text(ra.type), '_', ' '), ra.name), ''),
                                     nullif(concat_ws(' ', str.type, str.name, pod_detail.street_number), ''),
                                     nullif(concat('бл. ', pod_detail.block), 'бл. '),
                                     nullif(concat('вх. ', pod_detail.entrance), 'вх. '),
                                     nullif(concat('ет. ', pod_detail.floor), 'ет. '),
                                     nullif(concat('ап. ', pod_detail.apartment), 'ап. '),
                                     pod_detail.address_additional_info
                           )
                       else
                           concat_ws(', ',
                                     nullif(pod_detail.district_foreign, ''),
                                     nullif(concat_ws(' ',
                                                      replace(text(pod_detail.foreign_residential_area_type), '_', ' '),
                                                      pod_detail.residential_area_foreign), ''),
                                     nullif(
                                             concat_ws(' ', pod_detail.foreign_street_type, pod_detail.street_foreign,
                                                       pod_detail.street_number),
                                             ''),
                                     nullif(concat('бл. ', pod_detail.block), 'бл. '),
                                     nullif(concat('вх. ', pod_detail.entrance), 'вх. '),
                                     nullif(concat('ет. ', pod_detail.floor), 'ет. '),
                                     nullif(concat('ап. ', pod_detail.apartment), 'ап. '),
                                     pod_detail.address_additional_info
                           )
                       end                                                   as pod_address_comb,
                   case
                       when pod_detail.foreign_address
                           then pod_detail.populated_place_foreign
                       else (select pp.name from nomenclature.populated_places pp where pp.id = pod_detail.populated_place_id)
                       end                                                   as pod_place,
                   case
                       when pod_detail.foreign_address
                           then pod_detail.zip_code_foreign
                       else (select zc.zip_code from nomenclature.zip_codes zc where zc.id = pod_detail.zip_code_id)
                       end                                                   as pod_zip,
                   pc.name                                                   as price_component,
                   pc.don_not_include_in_the_vat_base                        as doNotIncludeVatBase,
                   dd.value                                                  as value,
                   mc.name                                                   as measure_unit_of_value,
                   dd.total_volumes                                          as total_volumes,
                   'бр.'                                                     as measure_unit_for_total_volumes,
                   dd.unit_price                                             as price,
                   cast(dd.unit_price * mc.alt_ccy_exchange_rate as decimal) as price_in_other_currency,
                   pcvt.name                                                 as measure_unit_of_price,
                   dd.vat_rate_percent                                       as vat_rate_percent,
                   (select sd.printing_name
                    from service_order.orders so
                             left join service.service_details sd on sd.id = so.service_detail_id
                    where so.id = inv.service_order_id)                      as product_name
            from invoice.invoices inv
                     left join invoice.invoice_detailed_data dd on dd.invoice_id = inv.id
                     left join pod.pod pod on pod.id = dd.pod_id
                     left join pod.pod_details pod_detail on (pod_detail.id = coalesce(dd.pod_detail_id, pod.last_pod_detail_id))
                     left join price_component.price_components pc on pc.id = dd.price_component_id
                     left join price_component.price_component_group_details pcgd on pcgd.id = dd.pc_group_detail_id
                     left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                     left join nomenclature.currencies mc on mc.id = dd.measure_unit_for_value_go
                     left join nomenclature.districts distr on distr.id = pod_detail.district_id
                     left join nomenclature.residential_areas ra on ra.id = pod_detail.residential_area_id
                     left join nomenclature.streets str on str.id = pod_detail.street_id
                     left join nomenclature.pod_measurement_types mt on mt.id = pod_detail.pod_measurement_types_id
            where inv.id = :invoiceId
              and inv.service_order_id is not null
              and (dd.pod_id is not null or dd.unrecognized_pod is not null)
            """)
    List<BillingRunDocumentDetailedDataProjection> getServiceOrderInvoiceDetailedData(Long invoiceId);

//Todo Measure unit of price in other currency
    @Query(value = """
            select
                pd.identifier as pod_id,
                pc.name as price_component,
                d.total_volumes as total_volumes,
                case
                    when d.detail_type = 'PER_PIECE' then
                        (select su.name from nomenclature.service_units su where d.measures_unit_for_total_volumes = su.id)
                    else (case
                              when (d.detail_type = 'SCALE' or d.detail_type = 'SETTLEMENT')
                                  then 'Kwh' end) end                               as measure_unit_for_total_volumes,
                d.unit_price as price,
                cast(d.unit_price * c.alt_ccy_exchange_rate as decimal)             as price_in_other_currency,
                (select pcvt.name
                 from nomenclature.price_component_value_types pcvt
                 where pc.price_component_value_type_id = pcvt.id) as measure_unit_of_price,
                d.main_currency_total_amount_without_vat value,
                c.name                                                              as measure_unit_of_value
            from invoice.invoice_standard_detailed_data_vat_base d
            join price_component.price_components pc on pc.id=d.pc_id
            join pod.pod pd on pd.id=d.pod_id
            join nomenclature.currencies c on c.id = d.main_currency_id
            where d.invoice_id=:invoiceId
            order by measure_unit_of_price, pc.name
            """,nativeQuery = true)
    List<BillingRunDocumentVatBaseProjection> findVatBaseDataByInvoiceId(Long invoiceId);
}
