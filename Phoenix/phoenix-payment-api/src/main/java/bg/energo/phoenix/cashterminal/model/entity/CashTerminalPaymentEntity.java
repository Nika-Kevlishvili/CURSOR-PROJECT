package bg.energo.phoenix.cashterminal.model.entity;

import bg.energo.phoenix.cashterminal.model.enums.CashTerminalPaymentStatus;
import bg.energo.phoenix.cashterminal.model.enums.CashTerminalRequestType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cash_terminal_payments", schema = "online_payment")
public class CashTerminalPaymentEntity {

    @Id
    @SequenceGenerator(
            name = "cash_terminal_payments_id_seq",
            sequenceName = "online_payment.cash_terminal_payments_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "cash_terminal_payments_id_seq"
    )
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "collection_channel_id", nullable = false)
    private Long collectionChannelId;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Column(name = "merchant_code", nullable = false)
    private String merchantCode;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_identifier", nullable = false)
    private String customerIdentifier;

    @Column(name = "billing_group_number")
    private String billingGroupNumber;

    @Column(name = "init_amount", nullable = false)
    private BigDecimal initAmount;

    @Column(name = "init_lfp_amount")
    private BigDecimal initLfpAmount;

    @Column(name = "init_total_amount", nullable = false)
    private BigDecimal initTotalAmount;

    @Column(name = "init_total_amount_in_coins", nullable = false)
    private Long initTotalAmountInCoins;

    @Column(name = "confirmed_amount")
    private BigDecimal confirmedAmount;

    @Column(name = "confirmed_total_amount_in_coins")
    private Long confirmedTotalCoinAmount;

    @Column(name = "status_code", nullable = false)
    private String statusCode;

    @Column(name = "is_combined", nullable = false)
    private Boolean isCombined;

    @Column(name = "init_date", nullable = false)
    private LocalDateTime initDate;

    @Column(name = "confirm_date")
    private LocalDateTime confirmDate;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CashTerminalPaymentStatus paymentStatus;

    @Column(name = "request_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CashTerminalRequestType requestType;

    @Column(name = "payment_id")
    private Long paymentId;
}

