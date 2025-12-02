package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "service_contract", name = "contract_unrecognized_pods")
public class ServiceUnrecognizedPods extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "contract_unrecognized_pods_seq",
            sequenceName = "service_contract.contract_unrecognized_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contract_unrecognized_pods_seq"
    )
    private Long id;

    @Column(name = "pod_identifier")
    private String podIdentifier;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ServiceSubobjectStatus status;

    @Column(name = "contract_detail_id")
    private Long contractDetailsId;
}
