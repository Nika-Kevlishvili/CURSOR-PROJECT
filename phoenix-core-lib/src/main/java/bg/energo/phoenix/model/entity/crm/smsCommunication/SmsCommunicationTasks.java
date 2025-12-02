package bg.energo.phoenix.model.entity.crm.smsCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_communication_tasks", schema = "crm")
public class SmsCommunicationTasks extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "sms_communication_tasks_id_seq",
            sequenceName = "crm.sms_communication_tasks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sms_communication_tasks_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "sms_communication_id")
    private Long smsCommunicationId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
