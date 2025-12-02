package bg.energo.phoenix.model.entity.product.termination.terminationGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.termination.terminationGroup.TerminationGroupStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "termination_groups", schema = "product")
public class TerminationGroup extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "termination_groups_id_seq",
            sequenceName = "product.termination_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "termination_groups_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TerminationGroupStatus status;

    public TerminationGroup(TerminationGroupStatus status) {
        this.status = status;
    }
}
