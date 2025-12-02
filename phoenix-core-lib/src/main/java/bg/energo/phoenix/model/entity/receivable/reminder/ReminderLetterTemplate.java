package bg.energo.phoenix.model.entity.receivable.reminder;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reminder_letter_templates", schema = "receivable")
public class ReminderLetterTemplate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "reminder_letter_templates_id_seq",
            schema = "receivable",
            sequenceName = "reminder_letter_templates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "reminder_letter_templates_id_seq"
    )
    private Long id;

    @Column(name = "reminder_id")
    private Long reminderId;

    @Column(name = "template_id")
    private Long templateId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

}
