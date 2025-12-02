package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.SupplierType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_disconnection_requests", schema = "receivable")
public class DisconnectionPowerSupplyRequests extends BaseEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "request_number")
    private String requestNumber;

    @Column(name = "supplier_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SupplierType supplierType;

    @Column(name = "disconnection_request_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DisconnectionRequestsStatus disconnectionRequestsStatus;

    @Column(name = "grid_operator_id")
    private Long gridOperatorId;

    @Column(name = "disconnection_reason_id")
    private Long disconnectionReasonId;

    @Column(name = "grid_operator_request_registration_date")
    private LocalDate gridOpRequestRegDate;

    @Column(name = "customer_reminder_letter_sent_date")
    private LocalDateTime customerReminderLetterSentDate;

    @Column(name = "grid_operator_disconnection_fee_pay_date")
    private LocalDate gridOpDisconnectionFeePayDate;

    @Column(name = "power_supply_disconnection_date")
    private LocalDate powerSupplyDisconnectionDate;

    @Column(name = "liability_amount_from")
    private BigDecimal liabilityAmountFrom;

    @Column(name = "liability_amount_to")
    private BigDecimal liabilityAmountTo;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "customer_condition_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CustomerConditionType customerConditionType;

    @Column(name = "customer_conditions")
    private String condition;

    @Column(name = "list_of_customers")
    private String listOfCustomers;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "power_supply_disconnection_reminder_id")
    private Long DisconnectionReminderId;

    @Column(name = "all_selected")
    private Boolean isAllSelected;

    @Column(name = "pods_with_highest_consumption")
    private Boolean podWithHighestConsumption;

    @Column(name = "exclude_pod_ids")
    private String excludePodIds;

    @Column(name = "tax_calculated")
    private Boolean taxCalculated;

}
