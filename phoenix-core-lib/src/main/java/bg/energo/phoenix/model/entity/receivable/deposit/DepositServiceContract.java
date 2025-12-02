package bg.energo.phoenix.model.entity.receivable.deposit;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "customer_deposit_service_contracts", schema = "receivable")
public class DepositServiceContract extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "customer_deposit_service_contracts_id_seq",
            sequenceName = "receivable.customer_deposit_service_contracts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_deposit_service_contracts_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_deposit_id")
    private Long customerDepositId;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;

}
