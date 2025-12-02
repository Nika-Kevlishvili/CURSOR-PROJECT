package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.SalesChannel;
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
@Table(schema = "product", name = "product_sales_channels")
public class ProductSalesChannel extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_sales_channels_seq", sequenceName = "product.product_sales_channels_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_sales_channels_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_id", referencedColumnName = "id")
    private ProductDetails productDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_channel_id", referencedColumnName = "id")
    private SalesChannel salesChannel;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductSubObjectStatus productSubObjectStatus;
}
