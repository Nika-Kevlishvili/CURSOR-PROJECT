package bg.energo.phoenix.model.entity.billing.invoice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "invoice", name = "invoice_detailed_data")
public class InvoiceDetailedData {
    @Id
    @SequenceGenerator(schema = "invoice", allocationSize = 1, name = "invoice_detailed_data_id", sequenceName = "invoice_detailed_data_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_detailed_data_id")
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "price_component_id")
    private Long priceComponentId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "unrecognized_pod")
    private String unrecognizedPod;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "meter_number")
    private String meterNumber;

    @Column(name = "new_meter_reading")
    private BigDecimal newMeterReading;

    @Column(name = "old_meter_reading")
    private BigDecimal oldMeterReading;

    @Column(name = "difference")
    private BigDecimal difference;

    @Column(name = "multiplier")
    private BigDecimal multiplier;

    @Column(name = "correction")
    private BigDecimal correction;

    @Column(name = "deducted")
    private BigDecimal deducted;

    @Column(name = "total_volumes")
    private BigDecimal totalVolumes;

    @Column(name = "measures_unit_for_total_volumes")
    private Long measureUnitForTotalVolumes;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "measure_unit_for_unit_price")
    private Long measureUnitForUnitPrice;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "measure_unit_for_value")
    private Long measureUnitForValue;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "vat_rate_percent")
    private BigDecimal vatRatePercent;

    @Column(name = "vat_rate_id")
    private Long vatRateId;

    @Column(name = "good_name")
    private String goodName;

    @Column(name = "measure_unit_for_total_volumes_go")
    private Long measureUnitForTotalVolumesGoodsOrder;

    @Column(name = "measure_unit_for_total_volumes_so")
    private Long measureUnitForTotalVolumesServiceOrder;

    @Column(name = "measure_unit_for_unit_price_go")
    private Long measureUnitForUnitPriceOrders;

    @Column(name = "measure_unit_for_value_go")
    private Long measureUnitForValueOrders;

    @Column(name = "pc_group_detail_id")
    private Long pcGroupDetailId;

    @Column(name = "currency_exchange_rate")
    private BigDecimal currencyExchangeRate;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;


    public InvoiceDetailedData cloneForReversal(Long invoiceId) {
        return new InvoiceDetailedData(
                null,
                invoiceId,
                this.priceComponentId,
                this.podId,
                this.unrecognizedPod,
                this.periodFrom,
                this.periodTo,
                this.meterNumber,
                this.newMeterReading,
                this.oldMeterReading,
                this.difference,
                this.multiplier,
                this.correction,
                this.deducted,
                this.totalVolumes,
                this.measureUnitForTotalVolumes,
                this.unitPrice,
                this.measureUnitForUnitPrice,
                this.value,
                this.measureUnitForValue,
                this.incomeAccountNumber,
                this.costCenterControllingOrder,
                this.vatRatePercent,
                this.vatRateId,
                this.goodName,
                this.measureUnitForTotalVolumesGoodsOrder,
                this.measureUnitForTotalVolumesServiceOrder,
                this.measureUnitForUnitPriceOrders,
                this.measureUnitForValueOrders,
                this.pcGroupDetailId,
                this.currencyExchangeRate,
                this.createDate
        );
    }
}
