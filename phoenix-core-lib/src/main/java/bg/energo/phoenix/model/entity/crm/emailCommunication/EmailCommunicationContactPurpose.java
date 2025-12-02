package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "email_communication_contact_purposes", schema = "crm")
public class EmailCommunicationContactPurpose extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communication_contact_purposes_id_seq"
    )
    @SequenceGenerator(
            name = "email_communication_contact_purposes_id_seq",
            sequenceName = "crm.email_communication_contact_purposes_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "email_communication_id")
    private Long emailCommunicationId;

    @Column(name = "contact_purpose_id")
    private Long contactPurposeId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}