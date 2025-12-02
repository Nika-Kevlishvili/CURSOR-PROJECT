package bg.energo.phoenix.model.entity.product.penalty.penaltyGroups;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "penalty_group_penalties", schema = "terms")
public class PenaltyGroupPenalty extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "penalty_group_penalties_id_seq",
            sequenceName = "terms.penalty_group_penalties_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "penalty_group_penalties_id_seq"
    )
    private Long id;

    @Column(name = "penalty_id")
    private Long penaltyId;

    @Column(name = "penalty_group_detail_id")
    private Long penaltyGroupDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
