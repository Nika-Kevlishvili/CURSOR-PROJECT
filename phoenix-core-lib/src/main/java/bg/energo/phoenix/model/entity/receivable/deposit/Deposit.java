package bg.energo.phoenix.model.entity.receivable.deposit;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customer_deposits", schema = "receivable")

public class Deposit extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "customer_deposits_id_seq",
            sequenceName = "receivable.customer_deposits_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_deposits_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "deposit_number")
    private String depositNumber;

    @Column(name = "payment_deadline")
    private LocalDate paymentDeadline;

    @Column(name = "refund_date")
    private LocalDate refundDate;

    @Column(name = "initial_amount")
    private BigDecimal initialAmount;

    @Column(name = "current_amount")
    private BigDecimal currentAmount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenter;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
