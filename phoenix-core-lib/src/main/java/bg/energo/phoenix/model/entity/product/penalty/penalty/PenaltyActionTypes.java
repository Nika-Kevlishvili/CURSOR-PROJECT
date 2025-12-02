package bg.energo.phoenix.model.entity.product.penalty.penalty;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Setter
@Entity
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "penalty_action_types", schema = "terms")
public class PenaltyActionTypes extends BaseEntity {
    @Id
    @SequenceGenerator(name = "penalty_action_types_id_seq", sequenceName = "terms.penalty_action_types_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "penalty_action_types_id_seq")
    private Long id;

    @Column(name = "penalty_id")
    private Long penaltyId;

    @Column(name = "action_type_id")
    private Long actionTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
