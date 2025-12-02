package bg.energo.phoenix.model.entity.product.penalty.penaltyGroups;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
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

@Table(name = "penalty_groups", schema = "terms")
public class PenaltyGroup extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "penalty_groups_id_seq",
            sequenceName = "terms.penalty_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "penalty_groups_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    public PenaltyGroup(EntityStatus status) {
        this.status = status;
    }
}
