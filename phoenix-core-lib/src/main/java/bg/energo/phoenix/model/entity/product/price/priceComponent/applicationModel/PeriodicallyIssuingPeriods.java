package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PeriodicallyIssuingPeriodsStatus;
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

@Table(name = "am_over_time_periodically_issuing_periods", schema = "price_component")
public class PeriodicallyIssuingPeriods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_over_time_periodically_issuing_periods_id_seq",
            sequenceName = "price_component.am_over_time_periodically_issuing_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_over_time_periodically_issuing_periods_id_seq"
    )
    private Long id;
    @Column(name = "period_from")
    private String periodFrom;
    @Column(name = "period_to")
    private String periodTo;
    @ManyToOne
    @JoinColumn(name = "am_over_time_periodically_id")
    private OverTimePeriodically overTimePeriodically;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PeriodicallyIssuingPeriodsStatus status;

    public PeriodicallyIssuingPeriods(OverTimePeriodically overTimePeriodically, PeriodOfYearBaseRequest request) {
        this.periodFrom = request.getStartDate();
        this.periodTo = request.getEndDate();
        this.overTimePeriodically = overTimePeriodically;
        this.status = PeriodicallyIssuingPeriodsStatus.ACTIVE;
    }
}
