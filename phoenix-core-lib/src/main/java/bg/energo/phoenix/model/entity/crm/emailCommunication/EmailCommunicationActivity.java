package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_communication_activity", schema = "crm")
public class EmailCommunicationActivity extends BaseEntity {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communication_activity_id_seq"
    )
    @SequenceGenerator(
            name = "email_communication_activity_id_seq",
            sequenceName = "crm.email_communication_activity_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "email_communication_id")
    private Long emailCommunicationId;

    @Column(name = "activity_id")
    private Long activityId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EntityStatus status;
}
