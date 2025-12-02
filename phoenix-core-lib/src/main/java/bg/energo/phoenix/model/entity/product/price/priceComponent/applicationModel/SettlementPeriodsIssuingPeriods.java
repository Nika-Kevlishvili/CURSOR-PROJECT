package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.PeriodOfYearBaseRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "am_for_volumes_by_settlement_period_issuing_periods", schema = "price_component")
public class SettlementPeriodsIssuingPeriods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_for_volumes_by_settlement_period_issuing_periods_id_seq",
            sequenceName = "price_component.am_for_volumes_by_settlement_period_issuing_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_for_volumes_by_settlement_period_issuing_periods_id_seq"
    )
    private Long id;

    @Column(name = "period_from")
    private String periodFrom;
    @Column(name = "period_to")
    private String periodTo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelSubObjectStatus status;
    @ManyToOne
    @JoinColumn(name = "am_for_volumes_by_settlement_period_id")
    private VolumesBySettlementPeriod volumesBySettlementPeriod;

    public SettlementPeriodsIssuingPeriods(VolumesBySettlementPeriod settlementPeriod, PeriodOfYearBaseRequest request) {
        this.periodFrom = request.getStartDate();
        this.periodTo = request.getEndDate();
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
        this.volumesBySettlementPeriod = settlementPeriod;
    }
}
