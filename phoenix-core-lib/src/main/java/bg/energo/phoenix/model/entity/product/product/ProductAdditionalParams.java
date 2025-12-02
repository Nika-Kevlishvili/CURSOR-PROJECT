package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "product", name = "product_additional_params")
public class ProductAdditionalParams extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_additional_params_id_seq", sequenceName = "product.product_additional_params_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_additional_params_id_seq")
    private Long id;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "label")
    private String label;

    @Column(name = "value")
    private String value;
}
