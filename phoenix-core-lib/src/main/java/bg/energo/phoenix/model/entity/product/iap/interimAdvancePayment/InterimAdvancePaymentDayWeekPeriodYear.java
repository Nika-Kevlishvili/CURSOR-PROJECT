package bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Week;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(schema = "interim_advance_payment", name = "interim_advance_payment_day_week_period_year")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class InterimAdvancePaymentDayWeekPeriodYear extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "interim_advance_payment_day_week_period_year_id_seq",
            sequenceName = "interim_advance_payment.interim_advance_payment_day_week_period_year_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interim_advance_payment_day_week_period_year_id_seq"
    )
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interim_advance_payment_id")
    private InterimAdvancePayment interimAdvancePayment;

    @Column(name = "week")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Week week;


    @Column(name = "day", columnDefinition = "interim_advance_payment.iap_day[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "interim_advance_payment.iap_day"
            )
    )
    private List<Day> days;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterimAdvancePaymentSubObjectStatus status;

}
