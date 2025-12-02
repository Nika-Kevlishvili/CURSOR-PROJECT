package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "service_contract", name = "contracts")
public class ServiceContractSubObject extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "contracts_pods_seq",
            sequenceName = "service_contract.contracts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contracts_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ServiceContractStatus status;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "contract_status")
    private ServiceContractDetailStatus contractStatus;

    @Column(name = "contract_sub_status")
    private ServiceContractDetailsSubStatus contractDetailsSubStatus;

    @Column(name = "contract_status_modify_date")
    private LocalDate contractStatusModifyDate;

    @Column(name = "activity_id")
    private Long activityId;
}
