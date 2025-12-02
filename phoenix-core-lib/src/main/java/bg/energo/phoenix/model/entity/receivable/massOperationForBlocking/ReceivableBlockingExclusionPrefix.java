package bg.energo.phoenix.model.entity.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mass_operation_for_blocking_exclution_prefixes", schema = "receivable")
public class ReceivableBlockingExclusionPrefix extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "mass_operation_for_blocking_exclution_prefixes_id_seq",
            sequenceName = "receivable.mass_operation_for_blocking_exclution_prefixes_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mass_operation_for_blocking_exclution_prefixes_id_seq"
    )
    private Long id;

    @Column(name = "mass_operation_for_blocking_id")
    private Long receivableBlockingId;

    @Column(name = "prefix_id")
    private Long prefixId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReceivableSubObjectStatus status;
}
