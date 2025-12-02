package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_communication_doc_templates", schema = "crm")
public class EmailCommunicationTemplates extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "email_communication_doc_templates_id_seq",
            sequenceName = "crm.email_communication_doc_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communication_doc_templates_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "email_communication_id")
    private Long emailCommId;


    @Column(name = "template_id")
    private Long templateId;

    public EmailCommunicationTemplates(Long templateId, Long emailCommId) {
        this.status = EntityStatus.ACTIVE;
        this.templateId = templateId;
        this.emailCommId = emailCommId;
    }
}
