package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "email_communication_customer_attachments", schema = "crm")
public class EmailCommunicationCustomerAttachment extends BaseEntity {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communication_customer_attachments_id_seq"
    )
    @SequenceGenerator(
            name = "email_communication_customer_attachments_id_seq",
            sequenceName = "crm.email_communication_customer_attachments_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Size(max = 200)
    @Column(name = "name", length = 200)
    private String name;

    @Size(max = 256)
    @Column(name = "file_url", length = 256)
    private String fileUrl;

    @Column(name = "email_communication_customer_id")
    private Long emailCommunicationCustomerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
