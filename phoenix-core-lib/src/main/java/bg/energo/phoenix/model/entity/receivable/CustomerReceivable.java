package bg.energo.phoenix.model.entity.receivable;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.OutgoingDocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_receivables",schema = "receivable")
@Builder
@AllArgsConstructor
public class CustomerReceivable extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_receivables_id_seq",
            sequenceName = "receivable.customer_receivables_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "customer_receivables_id_seq")
    private Long id;

    @Column(name = "receivable_number")
    private String receivableNumber;

    @Column(name = "account_period_id")
    private Long accountPeriodId;

    @Column(name = "full_offset_date")
    private LocalDate fullOffsetDate;

    @Column(name = "initial_amount")
    private BigDecimal initialAmount;

    @Column(name = "initial_amount_in_other_ccy")
    private BigDecimal initialAmountInOtherCurrency;

    @Column(name = "current_amount")
    private BigDecimal currentAmount;

    @Column(name = "current_amount_in_other_ccy")
    private BigDecimal currentAmountInOtherCurrency;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "outgoing_document_from_external_system")
    private String outgoingDocumentFromExternalSystem;

    @Column(name = "basis_for_issuing")
    private String basisForIssuing;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "iban")
    private String bankAccount;

    @Column(name = "blocked_for_payment")
    private Boolean blockedForPayment;

    @Column(name = "blocked_for_payment_from_date")
    private LocalDate blockedForPaymentFromDate;

    @Column(name = "blocked_for_payment_to_date")
    private LocalDate blockedForPaymentToDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "blocked_for_payment_blocking_reason_id")
    private Long blockedForPaymentBlockingReasonId;

    @Column(name = "blocked_for_payment_additional_info")
    private String blockedForPaymentAdditionalInformation;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "contract_billing_group_id")
    private Long billingGroupId;

    @Column(name = "alt_invoice_recipient_customer_id")
    private Long alternativeRecipientOfInvoiceId;

    @Column(name = "outgoing_document_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OutgoingDocumentType outgoingDocumentType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "creation_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CreationType creationType;

    @Column(name = "additional_info")
    private String additionalInformation;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "late_payment_fine_id")
    private Long latePaymentFineId;

}
