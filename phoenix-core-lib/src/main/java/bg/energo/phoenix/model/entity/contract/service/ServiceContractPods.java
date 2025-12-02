package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
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
@Table(schema = "service_contract", name = "contract_pods")
public class ServiceContractPods extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "contract_pods_seq",
            sequenceName = "service_contract.contract_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contract_pods_seq"
    )
    private Long id;


    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;

    @Column(name = "pod_id")
    private Long podId;
}
