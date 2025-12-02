package bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_disconnection_reminders", schema = "receivable")

public class PowerSupplyDisconnectionReminder extends BaseEntity{

    @Id
    @SequenceGenerator(
            name = "power_supply_disconnection_reminders_id_seq",
            sequenceName = "receivable.power_supply_disconnection_reminders_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_reminders_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "reminder_number")
    private String reminderNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reminder_status")
    private PowerSupplyDisconnectionReminderStatus reminderStatus;

    @Column(name = "customer_send_date")
    private LocalDateTime customerSendDate;

    @Column(name = "liability_amount_from")
    private BigDecimal liabilityAmountFrom;

    @Column(name = "liability_amount_to")
    private BigDecimal liabilityAmountTo;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "liabilities_max_due_date")
    private LocalDate liabilitiesMaxDueDate;

    @Column(name = "excluded_customer_list")
    private String excludedCustomerList;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.reminder_communication_channel"
            )
    )
    @Column(name = "communication_channel", columnDefinition = "receivable.reminder_communication_channel[]")
    private List<CommunicationChannel> communicationChannels;

    @Column(name = "document_template_id")
    private Long documentTemplateId;

    @Column(name = "email_template_id")
    private Long emailTemplateId;

    @Column(name = "sms_template_id")
    private Long smsTemplateId;

    @Column(name = "disconnection_date")
    private LocalDate disconnectionDate;

}
