package bg.energo.phoenix.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "easy_pay_verify_details", schema = "online_payment")
public class EasyPayVerifyDetailsEntity {

    @Id
    @SequenceGenerator(
            name = "easy_pay_verify_details_id_seq",
            sequenceName = "online_payment.easy_pay_verify_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "easy_pay_verify_details_id_seq"
    )
    private Long id;

    @Column(name = "easy_pay_verify_id", nullable = false)
    private Long easyPayVerifyId;

    @Column(name = "liability_id", nullable = false)
    private Long liabilityId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "lfp_amount")
    private BigDecimal lfpAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "total_amount_in_coins")
    private Long totalAmountInCoins;

}
