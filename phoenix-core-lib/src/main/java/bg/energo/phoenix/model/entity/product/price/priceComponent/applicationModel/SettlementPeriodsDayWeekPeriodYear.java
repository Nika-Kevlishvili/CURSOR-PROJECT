package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Week;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.DayOfWeekBaseRequest;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "am_for_volumes_by_settlement_periods_day_week_period_year", schema = "price_component")
public class SettlementPeriodsDayWeekPeriodYear extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_for_volumes_by_settlement_periods_day_week_period_year_id_seq",
            sequenceName = "price_component.am_for_volumes_by_settlement_periods_day_week_period_yea_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_for_volumes_by_settlement_periods_day_week_period_year_id_seq"
    )
    private Long id;

    @Column(name = "week")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Week week;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "price_component.am_day"
            )
    )
    @Column(name = "day", columnDefinition = "price_component.am_day[]")
    private List<Day> day;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelSubObjectStatus status;
    @ManyToOne
    @JoinColumn(name = "am_for_volumes_by_settlement_period_id")
    private VolumesBySettlementPeriod volumesBySettlementPeriod;

    public SettlementPeriodsDayWeekPeriodYear(VolumesBySettlementPeriod settlementPeriod, DayOfWeekBaseRequest request) {
        this.week = request.getWeek();
        this.day = request.getDays().stream().toList();
        this.status = ApplicationModelSubObjectStatus.ACTIVE;
        this.volumesBySettlementPeriod = settlementPeriod;
    }
}
