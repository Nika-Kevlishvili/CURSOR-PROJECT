package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "service", name = "services")
public class EPService extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "services_id_seq",
            sequenceName = "service.services_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "services_id_seq"
    )
    private Long id;

    @Column(name = "last_service_detail_id")
    private Long lastServiceDetailId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ServiceStatus status;

    @Column(name = "customer_identifier")
    private String customerIdentifier;

}
