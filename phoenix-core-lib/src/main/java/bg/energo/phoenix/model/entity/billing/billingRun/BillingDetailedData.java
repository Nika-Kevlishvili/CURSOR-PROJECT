package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "billing_detailed_data", schema = "billing")
public class BillingDetailedData extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_detailed_data_id_seq",
            sequenceName = "billing.billing_detailed_data_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_detailed_data_id_seq"
    )
    private Long id;

    @Column(name = "price_component")
    private String priceComponent;

    @Column(name = "pod")
    private String pod;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "meter")
    private String meter;

    @Column(name = "new_meter_reading")
    private BigDecimal newMeterReading;

    @Column(name = "old_meter_reading")
    private BigDecimal oldMeterReading;

    @Column(name = "differences")
    private BigDecimal differences;

    @Column(name = "multiplier")
    private BigDecimal multiplier;

    @Column(name = "correction")
    private BigDecimal correction;

    @Column(name = "deducted")
    private BigDecimal deducted;

    @Column(name = "total_volumes")
    private BigDecimal totalVolumes;

    @Column(name = "measures_unit_for_total_volumes")
    private String measuresUnitForTotalVolumes;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "measure_unit_for_unit_price")
    private String measureUnitForUnitPrice;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "value_currency_id")
    private Long valueCurrencyId;

    @Column(name = "income_account")
    private String incomeAccount;

    @Column(name = "cost_center")
    private String costCenter;

    @Column(name = "vat_rate_id")
    private Long vatRateId;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @Column(name = "billing_id")
    private Long billingId;

}
