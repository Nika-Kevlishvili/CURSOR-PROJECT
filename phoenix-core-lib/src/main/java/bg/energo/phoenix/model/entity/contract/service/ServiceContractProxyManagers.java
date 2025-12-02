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
@Table(schema = "service_contract", name = "contract_proxy_managers")
public class ServiceContractProxyManagers extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "service_contract_proxy_managers_seq",
            sequenceName = "service_contract.contract_proxy_managers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_contract_proxy_managers_seq"
    )
    private Long id;

    @Column(name = "contract_proxy_id")
    private Long contractProxyId;

    @Column(name = "customer_manager_id")
    private Long customerManagerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;
}

