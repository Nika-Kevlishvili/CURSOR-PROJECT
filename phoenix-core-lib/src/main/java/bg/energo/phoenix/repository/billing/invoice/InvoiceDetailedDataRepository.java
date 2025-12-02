package bg.energo.phoenix.repository.billing.invoice;

import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDetailedData;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedDataResponse;
import bg.energo.phoenix.model.response.billing.invoice.InvoiceSummaryDataMiddleResponse;
import bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDetailedDataRepository extends JpaRepository<InvoiceDetailedData, Long> {
    @Query(value = """
            select new bg.energo.phoenix.model.response.billing.invoice.InvoiceDetailedDataResponse(
                (
                    select pc.name from PriceComponent pc where pc.id = idd.priceComponentId
                ),
                case when idd.podId is not null then (
                    select pod.identifier
                    from PointOfDelivery pod
                    where pod.id = idd.podId
                ) else idd.unrecognizedPod end,
                idd.periodFrom,
                idd.periodTo,
                idd.meterNumber,
                idd.newMeterReading,
                idd.oldMeterReading,
                idd.unitPrice,
                (
                    select vt.name from PriceComponentValueType vt where vt.id = idd.measureUnitForUnitPrice
                ),
                idd.value,
                (
                    select cur.name from Currency cur where cur.id = idd.measureUnitForValueOrders
                ),
                idd.incomeAccountNumber,
                idd.costCenterControllingOrder,
                idd.vatRatePercent,
                idd.deducted,
                idd.multiplier,
                idd.difference,
                idd.correction,
                idd.totalVolumes,
                'бр.'
            )
            from InvoiceDetailedData idd
            where idd.invoiceId = :invoiceId
            and (idd.podId is not null or idd.unrecognizedPod is not null)
            order by idd.createDate
            """,
            countQuery = """
                    select count(idd.id)
                    from InvoiceDetailedData idd
                    where idd.invoiceId = :invoiceId
                    and (idd.podId is not null or idd.unrecognizedPod is not null)
                    """)
    Page<InvoiceDetailedDataResponse> findAllDetailedDataByInvoiceId(Long invoiceId, Pageable pageRequest);

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
                   groupDetailId,
                   pcId
            from (select pc.name                                                                       as name,
                         sum(d.total_volumes)                                                          as totalVolumes,
                         (select su.name from nomenclature.service_units su where d.measure_unit_for_total_volumes_so = su.id)
                                                                                                       as measureUnitForTotalVolumes,
                         d.unit_price                                                                  as unitPrice,
                         null                                                                          as measureUnitForUnitPrice,
                         sum(d.value)                                                                  as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.measure_unit_for_value_go)                                    as measureUnitForValue,
                         d.income_account_number                                                       as incomeAccountNumber,
                         d.cost_center_controlling_order                                               as costCenterControllingOrder,
                         d.vat_rate_percent                                                            as vatRate,
                         d.pc_group_detail_id                                                          as groupDetailId,
                         case when d.pc_group_detail_id is null then 'DIRECT' else 'FROM_PC_GROUP' end as type,
                         pc.id                                                                         as pcId
                  from invoice.invoice_detailed_data d
                           join price_component.price_components pc on pc.id = d.price_component_id
                  where d.invoice_id = :id and :invoiceObjectType = 'SERVICE_ORDER'
                  group by pc.name, d.unit_price, d.income_account_number, d.cost_center_controlling_order,
                           d.vat_rate_percent, d.pc_group_detail_id, pc.id, d.measure_unit_for_total_volumes_so,
                           d.measure_unit_for_value_go
                  union
                  select pcgd.name                                  as name,
                         null                                       as totalVolumes,
                         null                                       as measureUnitForTotalVolumes,
                         null                                       as unitPrice,
                         null                                       as measureUnitForUnitPrice,
                         sum(d.value)                               as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.measure_unit_for_value_go) as measureUnitForValue,
                         null                                       as incomeAccountNumber,
                         null                                       as costCenterControllingOrder,
                         null                                       as vatRate,
                         d.pc_group_detail_id                       as groupDetailId,
                         'GROUP'                                    as type,
                         null
                  from invoice.invoice_detailed_data d
                           join price_component.price_components pc on pc.id = d.price_component_id
                           join price_component.price_component_group_details pcgd on d.pc_group_detail_id = pcgd.id
                  where d.invoice_id = :id and :invoiceObjectType = 'SERVICE_ORDER'
                  group by pcgd.name, measureUnitForValue, d.pc_group_detail_id

                  union

                  select d.good_name                                         as name,
                         d.total_volumes                                     as totalVolumes,
                         (select gu.name
                          from nomenclature.goods_units gu
                          where gu.id = d.measure_unit_for_total_volumes_go) as measureUnitForTotalVolumes,
                         d.unit_price                                        as unitPrice,
                         null                                                as measureUnitForUnitPrice,
                         d.value                                             as value,
                         (select c.name
                          from nomenclature.currencies c
                          where c.id = d.measure_unit_for_value_go)          as measureUnitForValue,
                         d.income_account_number                             as incomeAccountNumber,
                         d.cost_center_controlling_order                     as costCenterControllingOrder,
                         d.vat_rate_percent                                  as vatRate,
                         null                                                as groupDetailId,
                         'DIRECT'                                            as type,
                         d.id                                                as pcId
                  from invoice.invoice_detailed_data d
                  where d.invoice_id = :id and :invoiceObjectType = 'GOODS_ORDER'
                 ) as tbl
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
                    """,
            countQuery = """
                    select count(tbl)
                    from (select pc.name                                                                       as name,
                                 sum(d.total_volumes)                                                          as totalVolumes,
                                 (select su.name from nomenclature.service_units su where d.measure_unit_for_total_volumes_so = su.id)
                                                                                                               as measureUnitForTotalVolumes,
                                 d.unit_price                                                                  as unitPrice,
                                 null                                                                          as measureUnitForUnitPrice,
                                 sum(d.value)                                                                  as value,
                                 (select c.name
                                  from nomenclature.currencies c
                                  where c.id = d.measure_unit_for_value_go)                                    as measureUnitForValue,
                                 d.income_account_number                                                       as incomeAccountNumber,
                                 d.cost_center_controlling_order                                               as costCenterControllingOrder,
                                 d.vat_rate_percent                                                            as vatRate,
                                 d.pc_group_detail_id                                                          as groupDetailId,
                                 case when d.pc_group_detail_id is null then 'DIRECT' else 'FROM_PC_GROUP' end as type,
                                 pc.id                                                                         as pcId
                          from invoice.invoice_detailed_data d
                                   join price_component.price_components pc on pc.id = d.price_component_id
                          where d.invoice_id = :id and :invoiceObjectType = 'SERVICE_ORDER'
                          group by pc.name, d.unit_price, d.income_account_number, d.cost_center_controlling_order,
                                   d.vat_rate_percent, d.pc_group_detail_id, pc.id, d.measure_unit_for_total_volumes_so,
                                   d.measure_unit_for_value_go
                          union
                          select pcgd.name                                  as name,
                                 null                                       as totalVolumes,
                                 null                                       as measureUnitForTotalVolumes,
                                 null                                       as unitPrice,
                                 null                                       as measureUnitForUnitPrice,
                                 sum(d.value)                               as value,
                                 (select c.name
                                  from nomenclature.currencies c
                                  where c.id = d.measure_unit_for_value_go) as measureUnitForValue,
                                 null                                       as incomeAccountNumber,
                                 null                                       as costCenterControllingOrder,
                                 null                                       as vatRate,
                                 d.pc_group_detail_id                       as groupDetailId,
                                 'GROUP'                                    as type,
                                 null
                          from invoice.invoice_detailed_data d
                                   join price_component.price_components pc on pc.id = d.price_component_id
                                   join price_component.price_component_group_details pcgd on d.pc_group_detail_id = pcgd.id
                          where d.invoice_id = :id and :invoiceObjectType = 'SERVICE_ORDER'
                          group by pcgd.name, measureUnitForValue, d.pc_group_detail_id

                          union

                          select d.good_name                                         as name,
                                 d.total_volumes                                     as totalVolumes,
                                 (select gu.name
                                  from nomenclature.goods_units gu
                                  where gu.id = d.measure_unit_for_total_volumes_go) as measureUnitForTotalVolumes,
                                 d.unit_price                                        as unitPrice,
                                 null                                                as measureUnitForUnitPrice,
                                 d.value                                             as value,
                                 (select c.name
                                  from nomenclature.currencies c
                                  where c.id = d.measure_unit_for_value_go)          as measureUnitForValue,
                                 d.income_account_number                             as incomeAccountNumber,
                                 d.cost_center_controlling_order                     as costCenterControllingOrder,
                                 d.vat_rate_percent                                  as vatRate,
                                 null                                                as groupDetailId,
                                 'DIRECT'                                            as type,
                                 d.id                                                as pcId
                          from invoice.invoice_detailed_data d
                          where d.invoice_id = :id and :invoiceObjectType = 'GOODS_ORDER'
                         ) as tbl
                    """)
    Page<InvoiceSummaryDataMiddleResponse> findAllSummaryDataByInvoiceId(Long id, String invoiceObjectType, Pageable pageRequest);

    @Query("""
            select new bg.energo.phoenix.service.billing.invoice.models.InvoiceDetailedDataAmountModel(
                idd.vatRatePercent,
                idd.value,
                c.mainCurrency,
                c.altCurrencyExchangeRate
            )
            from InvoiceDetailedData idd
            join Currency c on c.id = idd.measureUnitForValue
            where idd.invoiceId = :invoiceId
            """)
    List<InvoiceDetailedDataAmountModel> findInvoiceDetailedDataAmountsByInvoiceId(Long invoiceId);



    List<InvoiceDetailedData> findAllByInvoiceIdIn(List<Long> invoiceIds);

    List<InvoiceDetailedData> findAllByInvoiceId(Long invoiceId);
}
