package bg.energo.phoenix.model.entity;

import bg.energo.phoenix.model.enums.EasyPayRequestType;
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
@Table(name = "easy_pay_verify", schema = "online_payment")
public class EasyPayVerifyEntity {

    @Id
    @SequenceGenerator(
            name = "easy_pay_verify_id_seq",
            sequenceName = "online_payment.easy_pay_verify_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "easy_pay_verify_id_seq"
    )
    private Long id;

    @Column(name = "collection_channel_id", nullable = false)
    private Long collectionChannelId;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Column(name = "merchant_code", nullable = false)
    private String merchantCode;

    @Column(name = "customer_identifier", nullable = false)
    private String customerIdentifier;

    @Column(name = "billing_group_number")
    private String billingGroupNumber;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "lfp_amount")
    private BigDecimal lfpAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "total_amount_in_coins")
    private Long totalAmountInCoins;

    @Column(name = "status_code", nullable = false)
    private String statusCode;

    @Column(name = "verify_date")
    private LocalDateTime verifyDate;

    @Column(name = "request_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EasyPayRequestType requestType;

}
