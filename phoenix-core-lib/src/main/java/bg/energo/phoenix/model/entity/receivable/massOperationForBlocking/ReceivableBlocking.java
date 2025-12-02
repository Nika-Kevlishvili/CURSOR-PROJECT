package bg.energo.phoenix.model.entity.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingConditionType;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingType;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "receivable", name = "mass_operation_for_blocking")
public class ReceivableBlocking extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "mass_operation_for_blocking_id_seq",
            sequenceName = "receivable.mass_operation_for_blocking_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mass_operation_for_blocking_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.mass_operation_for_blocking_type"
            )
    )
    @Column(name = "type", columnDefinition = "receivable.mass_operation_for_blocking_type[]")
    private List<ReceivableBlockingType> blockingTypes;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "mass_operation_blocking_status")
    private ReceivableBlockingStatus blockingStatus;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_condition_type")
    private ReceivableBlockingConditionType blockingConditionType;

    @Column(name = "customer_conditions")
    private String customerConditions;

    @Column(name = "list_of_customers")
    private String listOfCustomers;

    @Column(name = "exclusion_by_amount_less_than")
    private BigDecimal lessThan;

    @Column(name = "exclusion_by_amount_greater_than")
    private BigDecimal greaterThan;

    //Relation
    @Column(name = "currency_id")
    private Long currencyId;

    //
    //Blocking for types
    //
    @Column(name = "blocked_for_payment")
    private Boolean blockedForPayment;

    @Column(name = "blocked_for_payment_from_date")
    private LocalDate blockedForPaymentFromDate;

    @Column(name = "blocked_for_payment_to_date")
    private LocalDate blockedForPaymentToDate;

    //Relation
    @Column(name = "blocked_for_payment_blocking_reason_id")
    private Long blockedForPaymentReasonId;

    @Column(name = "blocked_for_payment_additional_info")
    private String blockedForPaymentAdditionalInfo;

    @Column(name = "blocked_for_reminder_letters")
    private Boolean blockedForReminderLetters;

    @Column(name = "blocked_for_reminder_letters_from_date")
    private LocalDate blockedForReminderLettersFromDate;

    @Column(name = "blocked_for_reminder_letters_to_date")
    private LocalDate blockedForReminderLettersToDate;

    //Relation
    @Column(name = "blocked_for_reminder_letters_blocking_reason_id")
    private Long blockedForReminderLettersReasonId;

    @Column(name = "blocked_for_reminder_letters_additional_info")
    private String blockedForReminderLettersAdditionalInfo;

    @Column(name = "blocked_for_calculation_of_late_payment")
    private Boolean blockedForCalculations;

    @Column(name = "blocked_for_calculation_of_late_payment_from_date")
    private LocalDate blockedForCalculationsFromDate;

    @Column(name = "blocked_for_calculation_of_late_payment_to_date")
    private LocalDate blockedForCalculationsToDate;

    //Relation
    @Column(name = "blocked_for_calculation_of_late_payment_blocking_reason_id")
    private Long blockedForCalculationsReasonId;

    @Column(name = "blocked_for_calculation_of_late_payment_additional_info")
    private String blockedForCalculationsAdditionalInfo;

    @Column(name = "blocked_for_liabilities_offsetting")
    private Boolean blockedForLiabilitiesOffsetting;

    @Column(name = "blocked_for_liabilities_offsetting_from_date")
    private LocalDate blockedForLiabilitiesOffsettingFromDate;

    @Column(name = "blocked_for_liabilities_offsetting_to_date")
    private LocalDate blockedForLiabilitiesOffsettingToDate;

    //Relation
    @Column(name = "blocked_for_liabilities_offsetting_blocking_reason_id")
    private Long blockedForLiabilitiesOffsettingReasonId;

    @Column(name = "blocked_for_liabilities_offsetting_additional_info")
    private String blockedForLiabilitiesOffsettingAdditionalInfo;

    @Column(name = "blocked_for_supply_termination")
    private Boolean blockedForSupplyTermination;

    @Column(name = "blocked_for_supply_termination_from_date")
    private LocalDate blockedForSupplyTerminationFromDate;

    @Column(name = "blocked_for_supply_termination_to_date")
    private LocalDate blockedForSupplyTerminationToDate;

    //Relation
    @Column(name = "blocked_for_supply_termination_blocking_reason_id")
    private Long blockedForSupplyTerminationReasonId;

    @Column(name = "blocked_for_supply_termination_additional_info")
    private String blockedForSupplyTerminationAdditionalInfo;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EntityStatus status;

}
