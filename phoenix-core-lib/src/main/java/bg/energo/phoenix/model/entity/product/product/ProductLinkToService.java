package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.enums.product.product.ProductAllowSalesUnder;
import bg.energo.phoenix.model.enums.product.product.ProductObligatory;
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
@Table(schema = "product", name = "product_linked_services")
public class ProductLinkToService extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_linked_services_seq", sequenceName = "product.product_linked_services_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_linked_services_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private ProductDetails productDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_service_id", referencedColumnName = "id")
    private EPService linkedService;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "obligatory")
    private ProductObligatory obligatory;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "allows_sales_under")
    private ProductAllowSalesUnder allowSalesUnder;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductSubObjectStatus productSubObjectStatus;

    @Override
    public String toString() {
        return "ProductLinkToService{" +
                "id=" + id +
                ", productDetailId=" + productDetails.getId() +
                ", linkedServiceId=" + linkedService.getId() +
                ", obligatory=" + obligatory +
                ", allowSalesUnder=" + allowSalesUnder +
                ", productSubObjectStatus=" + productSubObjectStatus +
                '}';
    }
}
