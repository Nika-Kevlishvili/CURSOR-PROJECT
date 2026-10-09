package bg.energo.phoenix.cashterminal.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cash_terminal_payment_details", schema = "online_payment")
public class CashTerminalPaymentDetailsEntity {

    @Id
    @SequenceGenerator(
            name = "cash_terminal_payment_details_id_seq",
            sequenceName = "online_payment.cash_terminal_payment_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "cash_terminal_payment_details_id_seq"
    )
    private Long id;

    @Column(name = "cash_terminal_payment_id", nullable = false)
    private Long cashTerminalPaymentId;

    @Column(name = "liability_id", nullable = false)
    private Long liabilityId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "lfp_amount")
    private BigDecimal lfpAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "total_amount_in_coins")
    private Long totalAmountInCoins;
}

