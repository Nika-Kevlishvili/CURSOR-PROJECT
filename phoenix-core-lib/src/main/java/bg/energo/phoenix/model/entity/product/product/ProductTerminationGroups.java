package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroup;
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
@Table(schema = "product", name = "product_termination_groups")
public class ProductTerminationGroups extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_termination_groups_seq", sequenceName = "product.product_termination_groups_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "product_termination_groups_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_detail_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private ProductDetails productDetails;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_group_id", referencedColumnName = "id")
    private TerminationGroup terminationGroup;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductSubObjectStatus productSubObjectStatus;
}
