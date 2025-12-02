package bg.energo.phoenix.model.entity.receivable.customerLiability;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.CustomerLiabilitiesOutgoingDocType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_liabilities", schema = "receivable")
public class CustomerLiability extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_liabilities_id_seq",
            sequenceName = "receivable.customer_liabilities_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_liabilities_id_seq"
    )
    private Long id;

    @Column(name = "liability_number")
    private String liabilityNumber;

    @Column(name = "account_period_id")
    private Long accountPeriodId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "full_offset_date")
    private LocalDate fullOffsetDate;

    @Column(name = "applicable_interest_rate_id")
    private Long applicableInterestRateId;

    @Column(name = "applicable_interest_rate_date_from")
    private LocalDate applicableInterestRateDateFrom;

    @Column(name = "applicable_interest_rate_date_to")
    private LocalDate applicableInterestRateDateTo;

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
    private String iban;

    @Column(name = "blocked_for_payment")
    private Boolean blockedForPayment;

    @Column(name = "blocked_for_payment_from_date")
    private LocalDate blockedForPaymentFromDate;

    @Column(name = "blocked_for_payment_to_date")
    private LocalDate blockedForPaymentToDate;

    @Column(name = "blocked_for_payment_blocking_reason_id")
    private Long blockedForPaymentBlockingReasonId;

    @Column(name = "blocked_for_payment_additional_info")
    private String blockedForPaymentAdditionalInfo;

    @Column(name = "blocked_for_reminder_letters")
    private Boolean blockedForReminderLetters;

    @Column(name = "blocked_for_reminder_letters_from_date")
    private LocalDate blockedForReminderLettersFromDate;

    @Column(name = "blocked_for_reminder_letters_to_date")
    private LocalDate blockedForReminderLettersToDate;

    @Column(name = "blocked_for_reminder_letters_blocking_reason_id")
    private Long blockedForReminderLettersBlockingReasonId;

    @Column(name = "blocked_for_reminder_letters_additional_info")
    private String blockedForReminderLettersAdditionalInfo;

    @Column(name = "blocked_for_calculation_of_late_payment")
    private Boolean blockedForCalculationOfLatePayment;

    @Column(name = "blocked_for_calculation_of_late_payment_from_date")
    private LocalDate blockedForCalculationOfLatePaymentFromDate;

    @Column(name = "blocked_for_calculation_of_late_payment_to_date")
    private LocalDate blockedForCalculationOfLatePaymentToDate;

    @Column(name = "blocked_for_calculation_of_late_payment_blocking_reason_id")
    private Long blockedForCalculationOfLatePaymentBlockingReasonId;

    @Column(name = "blocked_for_calculation_of_late_payment_additional_info")
    private String blockedForCalculationOfLatePaymentAdditionalInfo;

    @Column(name = "blocked_for_liabilities_offsetting")
    private Boolean blockedForLiabilitiesOffsetting;

    @Column(name = "blocked_for_liabilities_offsetting_from_date")
    private LocalDate blockedForLiabilitiesOffsettingFromDate;

    @Column(name = "blocked_for_liabilities_offsetting_to_date")
    private LocalDate blockedForLiabilitiesOffsettingToDate;

    @Column(name = "blocked_for_liabilities_offsetting_blocking_reason_id")
    private Long blockedForLiabilitiesOffsettingBlockingReasonId;

    @Column(name = "blocked_for_liabilities_offsetting_additional_info")
    private String blockedForLiabilitiesOffsettingAdditionalInfo;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "deposit_id")
    private Long depositId;

    @Column(name = "contract_billing_group_id")
    private Long contractBillingGroupId;

    @Column(name = "alt_invoice_recipient_customer_id")
    private Long altInvoiceRecipientCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outgoing_document_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CustomerLiabilitiesOutgoingDocType outgoingDocumentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "creation_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CreationType creationType;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "blocked_for_supply_termination")
    private Boolean blockedForSupplyTermination;

    @Column(name = "blocked_for_supply_termination_from_date")
    private LocalDate blockedForSupplyTerminationFromDate;

    @Column(name = "blocked_for_supply_termination_to_date")
    private LocalDate blockedForSupplyTerminationToDate;

    @Column(name = "blocked_for_supply_termination_blocking_reason_id")
    private Long blockedForSupplyTerminationBlockingReasonId;

    @Column(name = "blocked_for_supply_termination_additional_info")
    private String blockedForSupplyTerminationAdditionalInfo;

    @Column(name = "late_payment_fine_id")
    private Long latePaymentFineId;

    @Column(name = "end_date_of_waiting_payment")
    private LocalDate endDateOfWaitingPayment;

    @Column(name = "child_late_payment_fine_id")
    private Long childLatePaymentFineId;

    @Column(name = "amount_without_interest")
    private BigDecimal amountWithoutInterest;

    @Column(name = "amount_without_interest_in_other_ccy")
    private BigDecimal amountWithoutInterestInOtherCurrency;

    @Column(name = "rescheduling_id")
    private Long reschedulingId;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "added_to_deposit")
    private Boolean addedToDeposit;

    @Column(name = "manual_liability_offsetting_id")
    private Long manualLiabilityOffsettingId;

}
