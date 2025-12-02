package bg.energo.phoenix.model.entity.product.price.priceComponentGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupPriceComponentStatus;
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

@Table(name = "pc_group_pcs", schema = "price_component")
public class PriceComponentGroupPriceComponent extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "pc_group_pcs_id_seq",
            sequenceName = "price_component.pc_group_pcs_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "pc_group_pcs_id_seq"
    )
    private Long id;

    @Column(name = "price_component_id")
    private Long priceComponentId;

    @Column(name = "price_component_group_detail_id")
    private Long priceComponentGroupDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PriceComponentGroupPriceComponentStatus status;

}
