package bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentSubObjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "interim_advance_payment", name = "interim_advance_payment_issuing_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder

public class InterimAdvancePaymentIssuingPeriod extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "interim_advance_payment_issuing_periods_id_seq",
            sequenceName = "interim_advance_payment.interim_advance_payment_issuing_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "interim_advance_payment_issuing_periods_id_seq"
    )
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interim_advance_payment_id")
    private InterimAdvancePayment interimAdvancePayment;

    @Column(name = "period_from")
    private String periodFrom;


    @Column(name = "period_to")
    private String periodTo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InterimAdvancePaymentSubObjectStatus status;

}
