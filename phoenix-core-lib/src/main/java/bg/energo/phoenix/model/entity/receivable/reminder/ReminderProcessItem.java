package bg.energo.phoenix.model.entity.receivable.reminder;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "process_reminder_items", schema = "process_management")
public class ReminderProcessItem extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "process_reminder_items_id_seq",
            schema = "process_management",
            sequenceName = "process_reminder_items_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_reminder_items_id_seq"
    )
    private Long id;

    @Column(name = "process_id")
    private Long processId;

    @Size(max = 255)
    @Column(name = "sent_status")
    private String sentStatus;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "reminder_id")
    private Long reminderId;

    @Column(name = "liability_id")
    private Long liabilityId;

    @Column(name = "customer_communication_contact_id")
    private Long customerCommunicationContactId;

    @Size(max = 512)
    @Column(name = "customer_communication_contact_value", length = 512)
    private String customerCommunicationContactValue;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "record_index")
    private Long recordIndex;

    @Column(name = "total_amount")
    private String totalAmount;

    @Column(name = "contact_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CustomerCommContactTypes contactType;

    @Column(name = "communication_id")
    private Long communicationId;

}