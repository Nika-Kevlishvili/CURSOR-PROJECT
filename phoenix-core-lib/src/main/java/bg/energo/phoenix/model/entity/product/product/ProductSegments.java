package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
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
@Table(schema = "product", name = "product_segments")
public class ProductSegments extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_segments_seq", sequenceName = "product.product_segments_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_segments_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_id", referencedColumnName = "id")
    private ProductDetails productDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", referencedColumnName = "id")
    private Segment segment;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductSubObjectStatus productSubObjectStatus;
}
