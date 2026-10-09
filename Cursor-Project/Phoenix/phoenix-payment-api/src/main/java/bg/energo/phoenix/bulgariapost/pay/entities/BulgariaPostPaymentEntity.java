package bg.energo.phoenix.bulgariapost.pay.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bulgaria_post_payments", schema = "online_payment")
public class BulgariaPostPaymentEntity {

    @Id
    @SequenceGenerator(
            name = "bulgaria_post_payments_id_seq",
            sequenceName = "online_payment.bulgaria_post_payments_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "bulgaria_post_payments_id_seq"
    )
    private Long id;

    @Column(name = "tid", nullable = false, length = 64, unique = true)
    private String tid;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "principal", nullable = false, precision = 20, scale = 2)
    private BigDecimal principal;

    @Column(name = "interest", nullable = false, precision = 20, scale = 2)
    private BigDecimal interest;

    @Column(name = "pay_date", nullable = false)
    private OffsetDateTime payDate;

}
