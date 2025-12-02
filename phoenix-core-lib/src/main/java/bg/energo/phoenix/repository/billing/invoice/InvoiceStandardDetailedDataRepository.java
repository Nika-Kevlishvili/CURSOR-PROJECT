package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.billingRun.model.StandardInvoiceForInterim;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceStandardDetailedData;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedDataProjection;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceSummaryDataMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceStandardDetailedDataRepository extends JpaRepository<InvoiceStandardDetailedData, Long> {

    @Query(value = """
               select round(sum(sd.mainCurrencyTotalAmountWithoutVat),2) as price,
                sd.vatRatePercent as percent,
                sd.mainCurrencyId as mainCurrencyId from InvoiceStandardDetailedData sd
               where sd.invoiceId =:invoiceId and sd.detailType <> 'INTERIM_DEDUCTION' and sd.mainCurrencyTotalAmountWithoutVat<>0
                    group by sd.vatRatePercent,sd.mainCurrencyId
            """)
    List<StandardInvoiceForInterim> findAllStandardDetailsByInvoiceId(Long invoiceId);

    List<InvoiceStandardDetailedData> findAllByInvoiceId(Long invoiceId);

    @Query(value = """
               select d from InvoiceStandardDetailedData d 
               where d.invoiceId = :invoiceId and d.detailType <> 'INTERIM_DEDUCTION'
            """)
    List<InvoiceStandardDetailedData> findAllByInvoiceIdForCorrection(Long invoiceId);


    @Query(nativeQuery = true, value = """
            select name,
                   totalVolumes,
                   measureUnitForTotalVolumes,
                   unitPrice,
                   measureUnitForUnitPrice,
                   value,
                   measureUnitForValue,
                   incomeAccountNumber,
                   costCenterControllingOrder,
                   vatRate,
                   type,
                   pcId
            from (select pc.name                                                                                               as name,
                         case when d.detail_type <> 'DISCOUNT' then sum(d.total_volumes) end                                   as totalVolumes,
                         case
                             when d.detail_type = 'PER_PIECE' then
                                 (select su.name from nomenclature.service_units su where d.measures_unit_for_total_volumes = su.id)
                             else (case
                                       when (d.detail_type = 'SCALE' or d.detail_type = 'SETTLEMENT')
                                           then 'kWh/kVArh' end) end                                                           as measureUnitForTotalVolumes,
                         case
                             when text(d.detail_type) in ('SETTLEMENT', 'SCALE') then
                                 round(sum(d.main_currency_total_amount_without_vat) /
                                       (case when sum(d.total_volumes) = 0 then 1 else sum(d.total_volumes) end), 12)
                             when text(d.detail_type) in
                                  ('PER_PIECE', 'OVER_TIME_ONE_TIME', 'OVER_TIME_PERIODICAL', 'WITH_ELECTRICITY')
                                 then round(sum(d.main_currency_total_amount_without_vat) /
                                            (case when sum(d.total_volumes) = 0 then 1 else sum(d.total_volumes) end),
                                            12) end                                                                            as unitPrice,
                         case
                             when (d.detail_type = 'SCALE' or d.detail_type = 'SETTLEMENT' or d.detail_type = 'PER_PIECE')
                                 then (select pcvt.name
                                       from nomenclature.price_component_value_types pcvt
                                       where pc.price_component_value_type_id = pcvt.id)
                             else case when (d.detail_type = 'DISCOUNT') then c.name end end                                   as measureUnitForUnitPrice,
                         sum(d.main_currency_total_amount_without_vat)                                                         as value,
                         c.name                                                                                                as measureUnitForValue,
                         d.income_account_number                                                                               as incomeAccountNumber,
                         d.cost_center_controlling_order                                                                       as costCenterControllingOrder,
                         d.vat_rate_percent                                                                                    as vatRate,
                         d.pc_group_detail_id                                                                                  as groupDetailId,
                         case
                             when d.pc_group_detail_id is null then 'DIRECT'
                             else 'FROM_PC_GROUP' end                                                                          as type,
                         pc.id                                                                                                 as pcId
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           join nomenclature.currencies c on c.id = d.main_currency_id
                  where d.invoice_id = :id
                  group by pc.name, d.income_account_number, d.cost_center_controlling_order,
                           d.vat_rate_percent, d.pc_group_detail_id, pc.id, d.detail_type, d.measures_unit_for_total_volumes,
                           d.main_currency_id, c.name
                  union
                  select coalesce(iap.name, 'Interim advance payment') as name,
                         null                                          as totalVolumes,
                         null                                          as measureUnitForTotalVolumes,
                         null                                          as unitPrice,
                         null                                          as measureUnitForUnitPrice,
                         d.main_currency_total_amount_without_vat      as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.main_currency_id)             as measureUnitForValue,
                         d.income_account_number                       as incomeAccountNumber,
                         d.cost_center_controlling_order               as costCenterControllingOrder,
                         d.vat_rate_percent                            as vatRate,
                         null                                          as groupDetailId,
                         'DIRECT'                                      as type,
                         d.interim_id                                  as pcId
                  from invoice.invoice_standard_detailed_data d
                           left join interim_advance_payment.interim_advance_payments iap on d.interim_id = iap.id
                  where d.invoice_id = :id
                    and d.detail_type = 'INTERIM_DEDUCTION'
                  union
                  select pcgd.name                                     as name,
                         null                                          as totalVolumes,
                         null                                          as measureUnitForTotalVolumes,
                         null                                          as unitPrice,
                         null                                          as measureUnitForUnitPrice,
                         sum(d.main_currency_total_amount_without_vat) as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.main_currency_id)             as measureUnitForValue,
                         null                                          as incomeAccountNumber,
                         null                                          as costCenterControllingOrder,
                         null                                          as vatRate,
                         d.pc_group_detail_id                          as groupDetailId,
                         'GROUP'                                       as type,
                         null
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           join price_component.price_component_group_details pcgd on d.pc_group_detail_id = pcgd.id
                  where d.invoice_id = :id
                  group by pcgd.name, measureUnitForValue, d.pc_group_detail_id) as tbl
            order by case
                         when groupDetailId is null then 1
                         else 2
                         end,
                     groupDetailId,
                     case
                         when type = 'GROUP' then 1
                         else 2
                         end,
                     pcId
            """, countQuery = """
            select count(1)
            from (select pc.name                                                             as name,
                         case when d.detail_type <> 'DISCOUNT' then sum(d.total_volumes) end as totalVolumes,
                         case
                             when d.detail_type = 'PER_PIECE' then
                                 (select su.name from nomenclature.service_units su where d.measures_unit_for_total_volumes = su.id)
                             else (case
                                       when (d.detail_type = 'SCALE' or d.detail_type = 'SETTLEMENT')
                                           then 'kWh/kVArh' end) end                         as measureUnitForTotalVolumes,
                         case
                             when text(d.detail_type) in ('SETTLEMENT', 'SCALE') then
                                 round(sum(d.main_currency_total_amount_without_vat) /
                                       (case when sum(d.total_volumes) = 0 then 1 else sum(d.total_volumes) end), 12)
                             when text(d.detail_type) in
                                  ('PER_PIECE', 'OVER_TIME_ONE_TIME', 'OVER_TIME_PERIODICAL', 'WITH_ELECTRICITY')
                                 then round(sum(d.main_currency_total_amount_without_vat) /
                                            (case when sum(d.total_volumes) = 0 then 1 else sum(d.total_volumes) end),
                                            12) end                                          as unitPrice,
                         case
                             when (d.detail_type = 'SCALE' or d.detail_type = 'SETTLEMENT' or d.detail_type = 'PER_PIECE')
                                 then (select pcvt.name
                                       from nomenclature.price_component_value_types pcvt
                                       where pc.price_component_value_type_id = pcvt.id)
                             else case when (d.detail_type = 'DISCOUNT') then c.name end end as measureUnitForUnitPrice,
                         sum(d.main_currency_total_amount_without_vat)                       as value,
                         c.name                                                              as measureUnitForValue,
                         d.income_account_number                                             as incomeAccountNumber,
                         d.cost_center_controlling_order                                     as costCenterControllingOrder,
                         d.vat_rate_percent                                                  as vatRate,
                         d.pc_group_detail_id                                                as groupDetailId,
                         case
                             when d.pc_group_detail_id is null then 'DIRECT'
                             else 'FROM_PC_GROUP' end                                        as type,
                         pc.id                                                               as pcId
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           join nomenclature.currencies c on c.id = d.main_currency_id
                  where d.invoice_id = :id
                  group by pc.name, d.income_account_number, d.cost_center_controlling_order,
                           d.vat_rate_percent, d.pc_group_detail_id, pc.id, d.detail_type, d.measures_unit_for_total_volumes,
                           d.main_currency_id, c.name
                  union
                  select coalesce(iap.name, 'Interim advance payment') as name,
                         null                                          as totalVolumes,
                         null                                          as measureUnitForTotalVolumes,
                         null                                          as unitPrice,
                         null                                          as measureUnitForUnitPrice,
                         -d.main_currency_total_amount_without_vat     as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.main_currency_id)             as measureUnitForValue,
                         d.income_account_number                       as incomeAccountNumber,
                         d.cost_center_controlling_order               as costCenterControllingOrder,
                         d.vat_rate_percent                            as vatRate,
                         null                                          as groupDetailId,
                         'DIRECT'                                      as type,
                         d.interim_id                                  as pcId
                  from invoice.invoice_standard_detailed_data d
                           left join interim_advance_payment.interim_advance_payments iap on d.interim_id = iap.id
                  where d.invoice_id = :id
                    and d.detail_type = 'INTERIM_DEDUCTION'
                  union
                  select pcgd.name                                     as name,
                         null                                          as totalVolumes,
                         null                                          as measureUnitForTotalVolumes,
                         null                                          as unitPrice,
                         null                                          as measureUnitForUnitPrice,
                         sum(d.main_currency_total_amount_without_vat) as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.main_currency_id)             as measureUnitForValue,
                         null                                          as incomeAccountNumber,
                         null                                          as costCenterControllingOrder,
                         null                                          as vatRate,
                         d.pc_group_detail_id                          as groupDetailId,
                         'GROUP'                                       as type,
                         null
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           join price_component.price_component_group_details pcgd on d.pc_group_detail_id = pcgd.id
                  where d.invoice_id = :id
                  group by pcgd.name, measureUnitForValue, d.pc_group_detail_id) as tbl
            """)
    Page<InvoiceSummaryDataMiddleResponse> findAllSummaryDataByInvoiceId(@Param("id") Long id, Pageable pageable);

    @Query(value = """
            select *
            from (select d.detail_type                                             as detail_type,
                         pc.name                                                   as price_component_name,
                         p.identifier                                              as pod_identifier,
                         d.date_from                                               as period_from,
                         d.date_to                                                 as period_to,
                         m.number                                                  as meter_number,
                         d.new_meter_reading                                       as new_meter_reading,
                         d.old_meter_reading                                       as old_meter_reading,
                         d.difference                                              as difference,
                         d.multiplier                                              as multiplier,
                         d.correction                                              as correction,
                         d.deducted                                                as deducted,
                         d.total_volumes                                           as total_volumes,
                         case when d.tariff = true then 'kWh/kVArh' else 'kWh' end as measure_of_total_volumes,
                         d.unit_price                                              as unit_price,
                         pcvt.name                                                 as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat                  as value,
                         c.name                                                    as measure_of_value,
                         d.income_account_number                                   as income_account_number,
                         d.cost_center_controlling_order                           as cost_center_controlling_order,
                         d.vat_rate_percent                                        as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('SCALE')
                    and d.price_component_price_type_id = 103
                    and (d.discounted = true or d.restricted = true)
                  union
                  select d.detail_type                                                                        as detail_type,
                         pc.name                                                                              as price_component_name,
                         p.identifier                                                                         as pod_identifier,
                         d.date_from                                                                          as period_from,
                         d.date_to                                                                            as period_to,
                         null                                                                                 as meter_number,
                         null                                                                                 as new_meter_reading,
                         null                                                                                 as old_meter_reading,
                         null                                                                                 as difference,
                         null                                                                                 as multiplier,
                         null                                                                                 as correction,
                         null                                                                                 as deducted,
                         sum(d.total_volumes)                                                                 as total_volumes,
                         ''                                                                                   as measure_of_total_volumes,
                         round(sum(d.main_currency_total_amount_without_vat) /
                               (case when sum(d.total_volumes) = 0 then 1 else sum(d.total_volumes) end), 12) as unit_price,
                         pcvt.name                                                                            as measure_of_unit_price,
                         sum(d.main_currency_total_amount_without_vat)                                        as value,
                         c.name                                                                               as measure_of_value,
                         d.income_account_number                                                              as income_account_number,
                         d.cost_center_controlling_order                                                      as cost_center_controlling_order,
                         d.vat_rate_percent                                                                   as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('SCALE')
                    and (d.price_component_price_type_id != 103
                      or (d.price_component_price_type_id = 103
                          and ((d.discounted is null or d.discounted = false) and (d.restricted is null or d.restricted = false)))
                      )
                  group by d.detail_type,
                           pc.name,
                           p.identifier,
                           d.date_from,
                           d.date_to,
                           pcvt.name,
                           c.name,
                           d.income_account_number,
                           d.cost_center_controlling_order,
                           d.vat_rate_percent,
                           pcpt.name
                  union
                  select d.detail_type                                             as detail_type,
                         pc.name                                                   as price_component_name,
                         p.identifier                                              as pod_identifier,
                         d.date_from                                               as period_from,
                         d.date_to                                                 as period_to,
                         m.number                                                  as meter_number,
                         d.new_meter_reading                                       as new_meter_reading,
                         d.old_meter_reading                                       as old_meter_reading,
                         d.difference                                              as difference,
                         d.multiplier                                              as multiplier,
                         d.correction                                              as correction,
                         d.deducted                                                as deducted,
                         d.total_volumes                                           as total_volumes,
                         case when d.tariff = true then 'kWh/kVArh' else 'kWh' end as measure_of_total_volumes,
                         d.unit_price                                              as unit_price,
                         pcvt.name                                                 as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat                  as value,
                         c.name                                                    as measure_of_value,
                         d.income_account_number                                   as income_account_number,
                         d.cost_center_controlling_order                           as cost_center_controlling_order,
                         d.vat_rate_percent                                        as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('SETTLEMENT')
                  union
                  select d.detail_type                                                                          as detail_type,
                         pc.name                                                                                as price_component_name,
                         case when d.unrecognized_pod is not null then d.unrecognized_pod else p.identifier end as pod_identifier,
                         d.date_from                                                                            as period_from,
                         d.date_to                                                                              as period_to,
                         m.number                                                                               as meter_number,
                         d.new_meter_reading                                                                    as new_meter_reading,
                         d.old_meter_reading                                                                    as old_meter_reading,
                         d.difference                                                                           as difference,
                         d.multiplier                                                                           as multiplier,
                         d.correction                                                                           as correction,
                         d.deducted                                                                             as deducted,
                         d.total_volumes                                                                        as total_volumes,
                         case
                             when (p.id is not null or d.unrecognized_pod is not null)
                                 then 'бр.' end                                                                 as measure_of_total_volumes,
                         case
                             when (d.pod_id is not null or d.unrecognized_pod is not null)
                                 then d.main_currency_total_amount_without_vat
                             else d.unit_price end                                                              as unit_price,
                         pcvt.name                                                                              as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat                                               as value,
                         c.name                                                                                 as measure_of_value,
                         d.income_account_number                                                                as income_account_number,
                         d.cost_center_controlling_order                                                        as cost_center_controlling_order,
                         d.vat_rate_percent                                                                     as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('PER_PIECE', 'OVER_TIME_ONE_TIME', 'OVER_TIME_PERIODICAL', 'WITH_ELECTRICITY')
                    and (d.pod_id is not null or d.unrecognized_pod is not null)
                  union
                  select d.detail_type                            as detail_type,
                         pc.name                                  as price_component_name,
                         p.identifier                             as pod_identifier,
                         d.date_from                              as period_from,
                         d.date_to                                as period_to,
                         m.number                                 as meter_number,
                         d.new_meter_reading                      as new_meter_reading,
                         d.old_meter_reading                      as old_meter_reading,
                         d.difference                             as difference,
                         d.multiplier                             as multiplier,
                         d.correction                             as correction,
                         d.deducted                               as deducted,
                         d.total_volumes                          as total_volumes,
                         'kWh'                                    as measure_of_total_volumes,
                         d.unit_price                             as unit_price,
                         pcvt.name                                as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat as value,
                         c.name                                   as measure_of_value,
                         d.income_account_number                  as income_account_number,
                         d.cost_center_controlling_order          as cost_center_controlling_order,
                         d.vat_rate_percent                       as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('DISCOUNT')) as detailed_data
            """, countQuery = """
            select count(1)
            from (select d.detail_type                                             as detail_type,
                         pc.name                                                   as price_component_name,
                         p.identifier                                              as pod_identifier,
                         d.date_from                                               as period_from,
                         d.date_to                                                 as period_to,
                         m.number                                                  as meter_number,
                         d.new_meter_reading                                       as new_meter_reading,
                         d.old_meter_reading                                       as old_meter_reading,
                         d.difference                                              as difference,
                         d.multiplier                                              as multiplier,
                         d.correction                                              as correction,
                         d.deducted                                                as deducted,
                         d.total_volumes                                           as total_volumes,
                         case when d.tariff = true then 'kWh/kVArh' else 'kWh' end as measure_of_total_volumes,
                         d.unit_price                                              as unit_price,
                         pcvt.name                                                 as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat                  as value,
                         c.name                                                    as measure_of_value,
                         d.income_account_number                                   as income_account_number,
                         d.cost_center_controlling_order                           as cost_center_controlling_order,
                         d.vat_rate_percent                                        as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('SCALE')
                    and d.price_component_price_type_id = 103
                    and (d.discounted = true or d.restricted = true)
                  union
                  select d.detail_type                                                                   as detail_type,
                         pc.name                                                                         as price_component_name,
                         p.identifier                                                                    as pod_identifier,
                         d.date_from                                                                     as period_from,
                         d.date_to                                                                       as period_to,
                         null                                                                            as meter_number,
                         null                                                                            as new_meter_reading,
                         null                                                                            as old_meter_reading,
                         null                                                                            as difference,
                         null                                                                            as multiplier,
                         null                                                                            as correction,
                         null                                                                            as deducted,
                         sum(d.total_volumes)                                                            as total_volumes,
                         ''                                                                              as measure_of_total_volumes,
                         round(sum(d.main_currency_total_amount_without_vat) / sum(d.total_volumes), 12) as unit_price,
                         pcvt.name                                                                       as measure_of_unit_price,
                         sum(d.main_currency_total_amount_without_vat)                                   as value,
                         c.name                                                                          as measure_of_value,
                         d.income_account_number                                                         as income_account_number,
                         d.cost_center_controlling_order                                                 as cost_center_controlling_order,
                         d.vat_rate_percent                                                              as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('SCALE')
                    and (d.price_component_price_type_id != 103
                      or (d.price_component_price_type_id = 103
                          and ((d.discounted is null or d.discounted = false) and (d.restricted is null or d.restricted = false)))
                      )
                  group by d.detail_type,
                           pc.name,
                           p.identifier,
                           d.date_from,
                           d.date_to,
                           pcvt.name,
                           c.name,
                           d.income_account_number,
                           d.cost_center_controlling_order,
                           d.vat_rate_percent,
                           pcpt.name
                  union
                  select d.detail_type                                             as detail_type,
                         pc.name                                                   as price_component_name,
                         p.identifier                                              as pod_identifier,
                         d.date_from                                               as period_from,
                         d.date_to                                                 as period_to,
                         m.number                                                  as meter_number,
                         d.new_meter_reading                                       as new_meter_reading,
                         d.old_meter_reading                                       as old_meter_reading,
                         d.difference                                              as difference,
                         d.multiplier                                              as multiplier,
                         d.correction                                              as correction,
                         d.deducted                                                as deducted,
                         d.total_volumes                                           as total_volumes,
                         case when d.tariff = true then 'kWh/kVArh' else 'kWh' end as measure_of_total_volumes,
                         d.unit_price                                              as unit_price,
                         pcvt.name                                                 as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat                  as value,
                         c.name                                                    as measure_of_value,
                         d.income_account_number                                   as income_account_number,
                         d.cost_center_controlling_order                           as cost_center_controlling_order,
                         d.vat_rate_percent                                        as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('SETTLEMENT')
                  union
                  select d.detail_type                                                                          as detail_type,
                         pc.name                                                                                as price_component_name,
                         case when d.unrecognized_pod is not null then d.unrecognized_pod else p.identifier end as pod_identifier,
                         d.date_from                                                                            as period_from,
                         d.date_to                                                                              as period_to,
                         m.number                                                                               as meter_number,
                         d.new_meter_reading                                                                    as new_meter_reading,
                         d.old_meter_reading                                                                    as old_meter_reading,
                         d.difference                                                                           as difference,
                         d.multiplier                                                                           as multiplier,
                         d.correction                                                                           as correction,
                         d.deducted                                                                             as deducted,
                         d.total_volumes                                                                        as total_volumes,
                         case
                             when (p.id is not null or d.unrecognized_pod is not null)
                                 then 'бр.' end                                                                 as measure_of_total_volumes,
                         case
                             when (d.pod_id is not null or d.unrecognized_pod is not null)
                                 then d.main_currency_total_amount_without_vat
                             else d.unit_price end                                                              as unit_price,
                         pcvt.name                                                                              as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat                                               as value,
                         c.name                                                                                 as measure_of_value,
                         d.income_account_number                                                                as income_account_number,
                         d.cost_center_controlling_order                                                        as cost_center_controlling_order,
                         d.vat_rate_percent                                                                     as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('PER_PIECE', 'OVER_TIME_ONE_TIME', 'OVER_TIME_PERIODICAL', 'WITH_ELECTRICITY')
                    and (d.pod_id is not null or d.unrecognized_pod is not null)
                  union
                  select d.detail_type                            as detail_type,
                         pc.name                                  as price_component_name,
                         p.identifier                             as pod_identifier,
                         d.date_from                              as period_from,
                         d.date_to                                as period_to,
                         m.number                                 as meter_number,
                         d.new_meter_reading                      as new_meter_reading,
                         d.old_meter_reading                      as old_meter_reading,
                         d.difference                             as difference,
                         d.multiplier                             as multiplier,
                         d.correction                             as correction,
                         d.deducted                               as deducted,
                         d.total_volumes                          as total_volumes,
                         'kWh'                                    as measure_of_total_volumes,
                         d.unit_price                             as unit_price,
                         pcvt.name                                as measure_of_unit_price,
                         d.main_currency_total_amount_without_vat as value,
                         c.name                                   as measure_of_value,
                         d.income_account_number                  as income_account_number,
                         d.cost_center_controlling_order          as cost_center_controlling_order,
                         d.vat_rate_percent                       as vat_rate_percent
                  from invoice.invoice_standard_detailed_data d
                           join price_component.price_components pc on pc.id = d.pc_id
                           left join pod.pod p on p.id = d.pod_id
                           left join nomenclature.currencies c on c.id = d.main_currency_id
                           left join pod.meters m on m.id = d.meter_id
                           left join nomenclature.price_component_value_types pcvt on pc.price_component_value_type_id = pcvt.id
                           left join nomenclature.price_component_price_types pcpt on pc.price_component_price_type_id = pcpt.id
                  where d.invoice_id = :invoiceId
                    and text(d.detail_type) in
                        ('DISCOUNT')) as detailed_data
            """, nativeQuery = true)
    Page<InvoiceDetailedDataProjection> findAllDetailedDataByInvoiceId(@Param("invoiceId") Long invoiceId, Pageable pageable);

    @Query(nativeQuery = true, value = """
            select (select iap.name
                    from interim_advance_payment.interim_advance_payments iap
                    where iap.id = d.interim_id)            as name,
                   d.main_currency_total_amount_without_vat as value,
                   (select c.name
                    from nomenclature.currencies c
                    where c.id = d.main_currency_id)        as measureUnitForValue,
                   d.income_account_number                  as incomeAccountNumber,
                   d.cost_center_controlling_order          as costCenterControllingOrder,
                   d.vat_rate_percent                       as vatRate,
                   'DIRECT'                                 as type
            from invoice.invoice_standard_detailed_data d
            where d.invoice_id = :id
            """,
            countQuery = """
                    select count(d.id)
                    from invoice.invoice_standard_detailed_data d
                    where d.invoice_id = :id
                    """)
    Page<InvoiceSummaryDataMiddleResponse> findAllInterimSummaryDataByInvoiceId(@Param("id") Long id, Pageable pageable);
}
