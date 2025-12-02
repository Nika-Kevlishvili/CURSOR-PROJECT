package bg.energo.phoenix.model.entity.nomenclature.product.priceComponent;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.priceComponent.PriceComponentValueTypeRequest;
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

@Table(name = "price_component_value_types", schema = "nomenclature")
public class PriceComponentValueType extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "id_seq",
            sequenceName = "nomenclature.price_component_value_types_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public PriceComponentValueType(PriceComponentValueTypeRequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.defaultSelection = request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection();
    }
}
