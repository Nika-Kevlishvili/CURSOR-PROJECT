package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
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

@Table(schema = "service", name = "service_terminations")
public class ServiceTermination extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_terminations_id_seq",
            sequenceName = "service.service_terminations_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_terminations_id_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_detail_id", referencedColumnName = "id")
    private ServiceDetails serviceDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_id", referencedColumnName = "id")
    private Termination termination;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ServiceSubobjectStatus status;


    public ServiceTermination(ServiceDetails serviceDetails, Termination termination, ServiceSubobjectStatus status) {
        this.serviceDetails = serviceDetails;
        this.termination = termination;
        this.status = status;
    }
}
