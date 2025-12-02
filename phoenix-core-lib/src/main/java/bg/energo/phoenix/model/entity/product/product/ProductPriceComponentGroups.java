package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroup;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)

@Entity
@Table(schema = "product", name = "product_price_component_groups")
public class ProductPriceComponentGroups extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_price_component_groups_seq", sequenceName = "product.product_price_component_groups_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_price_component_groups_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude

    private ProductDetails productDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_component_group_id", referencedColumnName = "id")
    private PriceComponentGroup priceComponentGroup;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductSubObjectStatus productSubObjectStatus;
}
