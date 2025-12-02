package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "product", name = "products")
public class Product extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_id_seq", sequenceName = "product.product_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_id_seq")
    private Long id;

    @Column(name = "last_product_detail_id")
    private Long lastProductDetail;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus productStatus;

    @Column(name = "customer_identifier")
    private String customerIdentifier;
}
