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
@Table(name = "reminder_exclude_liabilities_by_prefixes", schema = "receivable")
public class ExcludeLiabilitiesByPrefix extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "reminder_exclude_liabilities_by_prefixes_id_seq",
            schema = "receivable",
            sequenceName = "reminder_exclude_liabilities_by_prefixes_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "reminder_exclude_liabilities_by_prefixes_id_seq"
    )
    private Long id;

    @Column(name = "reminder_id")
    private Long reminderId;

    @Column(name = "prefix_id")
    private Long prefixId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

}
