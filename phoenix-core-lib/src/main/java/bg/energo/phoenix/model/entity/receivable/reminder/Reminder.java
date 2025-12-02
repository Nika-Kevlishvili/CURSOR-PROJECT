package bg.energo.phoenix.model.entity.receivable.reminder;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reminder.CommunicationChannel;
import bg.energo.phoenix.model.enums.receivable.reminder.ReminderConditionType;
import bg.energo.phoenix.model.enums.receivable.reminder.TriggerForLiabilities;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reminders", schema = "receivable")
public class Reminder extends BaseEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "reminder_number")
    private String number;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "trigger_for_liabilities")
    private TriggerForLiabilities triggerForLiabilities;

    @Column(name = "postponement_in_days")
    private Integer postponementInDays;

    @Column(name = "due_amount_from")
    private BigDecimal dueAmountFrom;

    @Column(name = "due_amount_to")
    private BigDecimal dueAmountTo;

    @Column(name = "currency_id")
    private Long currencyId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "customer_condition")
    private ReminderConditionType customerConditionType;

    @Column(name = "customer_under_conditions")
    private String conditions;

    @Column(name = "list_of_customers")
    private String listOfCustomers;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.reminder_communication_channel"
            )
    )
    @Column(name = "communication_channel", columnDefinition = "receivable.reminder_communication_channel[]")
    private List<CommunicationChannel> communicationChannels;

    @Column(name = "contact_purpose_id")
    private Long contactPurposeId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

    @Column(name = "template_id")
    private Long documentTemplateId;

    @Column(name = "email_template_id")
    private Long emailTemplateId;

    @Column(name = "sms_template_id")
    private Long smsTemplateId;
}
