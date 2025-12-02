package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.PenaltyGroup;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor

@Table(schema = "service", name = "service_penalty_groups")
public class ServicePenaltyGroup extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_penalty_groups_id_seq",
            sequenceName = "service.service_penalty_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_penalty_groups_id_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_detail_id", referencedColumnName = "id")
    private ServiceDetails serviceDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_group_id", referencedColumnName = "id")
    private PenaltyGroup penaltyGroup;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ServiceSubobjectStatus status;

}
