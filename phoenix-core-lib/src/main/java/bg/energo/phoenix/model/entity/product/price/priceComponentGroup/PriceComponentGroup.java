package bg.energo.phoenix.model.entity.product.price.priceComponentGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupStatus;
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

@Table(name = "price_component_groups", schema = "price_component")
public class PriceComponentGroup extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "price_component_groups_id_seq",
            sequenceName = "price_component.price_component_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "price_component_groups_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PriceComponentGroupStatus status;

}
