package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
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

@Table(schema = "service", name = "service_penalties")
public class ServicePenalty extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_penalties_id_seq",
            sequenceName = "service.service_penalties_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_penalties_id_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_detail_id", referencedColumnName = "id")
    private ServiceDetails serviceDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id", referencedColumnName = "id")
    private Penalty penalty;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ServiceSubobjectStatus status;

}
