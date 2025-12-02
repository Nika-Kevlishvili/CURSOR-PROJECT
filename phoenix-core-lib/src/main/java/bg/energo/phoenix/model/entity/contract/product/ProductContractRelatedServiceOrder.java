package bg.energo.phoenix.model.entity.contract.product;

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

@Table(name = "contract_related_service_orders", schema = "product_contract")
public class ProductContractRelatedServiceOrder extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "product_contract_related_service_orders_id_seq",
            sequenceName = "contract_related_service_orders_id_seq",
            schema = "product_contract",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_contract_related_service_orders_id_seq"
    )
    private Long id;

    @Column(name = "contract_id")
    private Long productContractId;

    @Column(name = "service_order_id")
    private Long serviceOrderId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
