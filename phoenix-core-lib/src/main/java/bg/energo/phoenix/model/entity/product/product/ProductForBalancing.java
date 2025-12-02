package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.response.communication.xEnergie.BalancingSystemsProducts;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "product", name = "product_for_balancing")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class ProductForBalancing extends BaseEntity {
    @Id
    @GeneratedValue(generator = "product_for_balancing_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "product_for_balancing_seq", schema = "product", allocationSize = 1, sequenceName = "product_for_balancing_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    public ProductForBalancing(BalancingSystemsProducts balancingSystemsProducts) {
        this.name = balancingSystemsProducts.name();
        this.status = EntityStatus.ACTIVE;
    }
}
