package bg.energo.phoenix.model.entity.pod.billingByScale;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "billing_data_by_scale", schema = "pod")
public class BillingDataByScale extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_data_by_scale_id_seq",
            sequenceName = "pod.billing_data_by_scale_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_data_by_scale_id_seq"
    )
    private Long id;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(name = "meter_id")
    private Long meterId;

    @Column(name = "scale_id")
    private Long scaleId;

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "new_meter_reading")
    private BigDecimal newMeterReading;

    @Column(name = "old_meter_reading")
    private BigDecimal oldMeterReading;

    @Column(name = "difference_kwh")
    private BigDecimal differenceKwh;

    @Column(name = "multiplier")
    private BigDecimal multiplier;

    @Column(name = "correction_kwh")
    private BigDecimal correctionKwh;

    @Column(name = "deducted_kwh")
    private BigDecimal deductedKwh;

    @Column(name = "total_volumes_kwh")
    private BigDecimal totalVolumesKwh;

    @Column(name = "volumes")
    private BigDecimal volumes;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    @Column(name = "billing_by_scale_id")
    private Long billingByScaleId;

    @Column(name = "scale_number")
    private String scaleNumber;

    @Column(name="index")
    private Integer index;
}
