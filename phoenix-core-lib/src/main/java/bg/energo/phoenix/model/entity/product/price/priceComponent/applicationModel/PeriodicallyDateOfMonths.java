package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PeriodicallyDateOfMonthsStatus;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.DateOfMonthBaseRequest;
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
@Table(name = "am_over_time_periodically_date_of_months", schema = "price_component")
public class PeriodicallyDateOfMonths extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_over_time_periodically_date_of_months_id_seq",
            sequenceName = "price_component.am_over_time_periodically_date_of_months_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_over_time_periodically_date_of_months_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PeriodicallyDateOfMonthsStatus status;

    @ManyToOne
    @JoinColumn(name = "am_over_time_periodically_id")
    private OverTimePeriodically overTimePeriodically;
    @Column(name = "month")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Month month;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "price_component.am_month_number"
            )
    )
    @Column(name = "month_number", columnDefinition = "price_component.am_month_number[]")
    private List<MonthNumber> monthNumber;

    public PeriodicallyDateOfMonths(OverTimePeriodically overTimePeriodically, DateOfMonthBaseRequest request) {
        this.status = PeriodicallyDateOfMonthsStatus.ACTIVE;
        this.overTimePeriodically = overTimePeriodically;
        this.month = request.getMonth();
        this.monthNumber = request.getMonthNumbers().stream().toList();
    }
}
