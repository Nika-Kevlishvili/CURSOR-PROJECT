package bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(schema = "interim_advance_payment", name = "interim_advance_payment_date_of_months")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class InterimAdvancePaymentDateOfMonth extends BaseEntity {


    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "interim_advance_payment_date_of_months_id_seq",
            sequenceName = "interim_advance_payment.interim_advance_payment_date_of_months_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interim_advance_payment_date_of_months_id_seq"
    )
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interim_advance_payment_id")
    private InterimAdvancePayment interimAdvancePayment;

    @Column(name = "month")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Month month;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "interim_advance_payment.iap_month_number"
            )
    )
    @Column(name = "month_number", columnDefinition = "interim_advance_payment.iap_month_number[]")
    private List<MonthNumber> monthNumbers;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterimAdvancePaymentSubObjectStatus status;

}
