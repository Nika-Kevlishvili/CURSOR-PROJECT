package bg.energo.phoenix.model.entity.contract.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "contracts", schema = "product_contract")
public class Contract extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_seq")
    @SequenceGenerator(name = "contract_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}