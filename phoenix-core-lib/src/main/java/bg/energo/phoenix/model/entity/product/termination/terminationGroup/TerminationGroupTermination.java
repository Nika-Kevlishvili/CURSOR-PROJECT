package bg.energo.phoenix.model.entity.product.termination.terminationGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.termination.terminationGroup.TerminationGroupTerminationStatus;
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

@Table(name = "termination_group_terminations", schema = "product")
public class TerminationGroupTermination extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "termination_group_terminations_id_seq",
            sequenceName = "product.termination_group_terminations_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "termination_group_terminations_id_seq"
    )
    private Long id;

    @Column(name = "termination_id")
    private Long terminationId;

    @Column(name = "termination_group_detail_id")
    private Long terminationGroupDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TerminationGroupTerminationStatus status;

}
