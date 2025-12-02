package bg.energo.phoenix.model.entity.crm.smsCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.CommunicationType;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_communications", schema = "crm")
public class SmsCommunication extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "sms_communications_id_seq",
            sequenceName = "crm.sms_communications_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sms_communications_id_seq")
    @Column(name = "id")
    private Long id;


    @Column(name = "communication_as_an_institution")
    private boolean communicationAsInstitution;

    @Column(name = "communication_topic_id")
    private Long communicationTopicId;

    @Column(name = "communication_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CommunicationType communicationType;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "sms_sending_number_id")
    private Long smsSendingNumberId;

    @Column(name = "sms_body")
    private String smsBody;

    @Column(name = "sender_employee_id")
    private Long senderEmployeeId;

    @Column(name = "communication_channel")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SmsCommunicationChannel communicationChannel;

    @Column(name = "communication_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SmsCommStatus communicationStatus;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "all_customers_with_active_contract")
    private boolean allCustomersWithActiveContract;
}
