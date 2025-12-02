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
@Table(name = "action_liabilities", schema = "action")
public class ActionLiability extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "action_liabilities_id_seq",
            sequenceName = "action.action_liabilities_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "action_liabilities_id_seq"
    )
    private Long id;

    @Column(name = "action_id")
    private Long actionId;

    // TODO: 11/22/23 add other fields here, when the liability story will be implemented (update schema.sql too)

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
