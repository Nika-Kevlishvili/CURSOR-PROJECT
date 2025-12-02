package bg.energo.phoenix.model.entity.receivable.payment;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.payment.OutgoingDocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "customer_payments", schema = "receivable")

public class Payment extends BaseEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "payment_number")
    private String paymentNumber;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "full_offset_date")
    private LocalDate fullOffsetDate;

    @Column(name = "initial_amount")
    private BigDecimal initialAmount;

    @Column(name = "current_amount")
    private BigDecimal currentAmount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "collection_channel_id")
    private Long collectionChannelId;

    @Column(name = "payment_package_id")
    private Long paymentPackageId;

    @Column(name = "account_period_id")
    private Long accountPeriodId;

    @Column(name = "payment_purpose")
    private String paymentPurpose;

    @Column(name = "payment_info")
    private String paymentInfo;

    @Column(name = "blocked_for_offsetting")
    private Boolean blockedForOffsetting;

    @Column(name = "blocked_for_offsetting_from_date")
    private LocalDate blockedForOffsettingFromDate;

    @Column(name = "blocked_for_offsetting_to_date")
    private LocalDate blockedForOffsettingToDate;

    @Column(name = "blocked_for_offsetting_blocking_reason_id")
    private Long blockedForOffsettingBlockingReasonId;

    @Column(name = "blocked_for_offsetting_additional_info")
    private String blockedForOffsettingAdditionalInfo;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "contract_billing_group_id")
    private Long contractBillingGroupId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "outgoing_document_type")
    private OutgoingDocumentType outgoingDocumentType;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "late_payment_fine_id")
    private Long latePaymentFineId;

    @Column(name = "customer_deposit_id")
    private Long customerDepositId;

    @Column(name = "penalty_id")
    private Long penaltyId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;
}
