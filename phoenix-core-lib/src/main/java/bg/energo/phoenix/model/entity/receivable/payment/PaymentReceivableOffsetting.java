package bg.energo.phoenix.model.entity.receivable.payment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "customer_payment_receivable_offsettings", schema = "receivable")
public class PaymentReceivableOffsetting extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_payment_receivable_offsettings_id_seq",
            schema = "receivable",
            sequenceName = "customer_payment_receivable_offsettings_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_payment_receivable_offsettings_id_seq"
    )
    private Long id;

    @Column(name = "customer_payment_id")
    private Long customerPaymentId;

    @Column(name = "customer_receivable_id")
    private Long customerReceivableId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;
}
