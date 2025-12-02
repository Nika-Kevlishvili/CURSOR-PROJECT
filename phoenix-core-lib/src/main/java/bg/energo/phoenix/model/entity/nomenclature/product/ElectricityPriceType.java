package bg.energo.phoenix.model.entity.nomenclature.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.ElectricityPriceTypeRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "electricity_price_types", schema = "nomenclature")
public class ElectricityPriceType extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "electricity_price_types_seq",
            sequenceName = "nomenclature.electricity_price_types_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "electricity_price_types_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    public ElectricityPriceType(ElectricityPriceTypeRequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.defaultSelection =request.getStatus().equals(NomenclatureItemStatus.ACTIVE) && request.getDefaultSelection();
    }
}
