package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationChannelType;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationDocGenerationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationStatus;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationType;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Types;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_communications", schema = "crm")
public class EmailCommunication extends BaseEntity {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communications_id_seq"
    )
    @SequenceGenerator(
            name = "email_communications_id_seq",
            sequenceName = "crm.email_communications_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "communication_as_an_institution")
    @Builder.Default
    private Boolean communicationAsAnInstitution = false;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_channel")
    private EmailCommunicationChannelType communicationChannel;

    @Size(max = 128)
    @Column(name = "dms_number", length = 128)
    private String dmsNumber;

    @Column(name = "communication_topic_id")
    private Long communicationTopicId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_type")
    private EmailCommunicationType emailCommunicationType;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_status")
    private EmailCommunicationStatus emailCommunicationStatus;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "email_mailbox_id")
    private Long emailBoxId;

    @Size(max = 255)
    @Column(name = "email_subject", length = 255)
    private String emailSubject;

    @Column(name = "email_body")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String emailBody;

    @Column(name = "sender_employee_id")
    private Long senderEmployeeId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EntityStatus entityStatus;

    @Column(name = "email_template_id")
    private Long emailTemplateId;

    @Column(name = "creation_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CreationType creationType;

    @Column(name = "doc_generation_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EmailCommunicationDocGenerationStatus docGenerationStatus;

}