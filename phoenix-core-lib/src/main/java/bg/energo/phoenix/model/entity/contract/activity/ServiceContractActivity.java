package bg.energo.phoenix.model.entity.contract.activity;

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
@Table(name = "contract_activity", schema = "service_contract")
public class ServiceContractActivity extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_contract_activity_id_seq",
            sequenceName = "service_contract.contract_activity_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_contract_activity_id_seq"
    )
    private Long id;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "activity_id")
    private Long systemActivityId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
