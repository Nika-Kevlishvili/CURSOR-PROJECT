package bg.energo.phoenix.model.entity.contract.action;

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
@Table(name = "action_pods", schema = "action")
public class ActionPod extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "action_pods_id_seq",
            sequenceName = "action.action_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "action_pods_id_seq"
    )
    private Long id;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "action_id")
    private Long actionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;

}
