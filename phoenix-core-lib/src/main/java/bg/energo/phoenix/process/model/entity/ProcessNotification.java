package bg.energo.phoenix.process.model.entity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.process.model.enums.ProcessNotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "process_notification", schema = "process_management")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessNotification extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    @SequenceGenerator(
            name = "process_notification_id_seq",
            sequenceName = "process_management.process_notification_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_notification_id_seq"
    )
    private Long id;


    @Column(name = "process_id")
    private Long processId;

    @Column(name = "performer_tag_id")
    private Long performerTagId;

    @Column(name = "performer_id")
    private Long performerId;

    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProcessNotificationType notificationType;
}
