package bg.energo.phoenix.model.entity.nomenclature.product.priceComponent;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.priceComponent.ScalesRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
@Entity
@Table(schema = "nomenclature", name = "scales")
public class Scales extends BaseEntity {
    @Id
    @SequenceGenerator(name = "scales_id_seq", schema = "nomenclature", sequenceName = "scales_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "scales_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_operator_id", referencedColumnName = "id")
    private GridOperator gridOperator;

    @Column(name = "scale_type")
    private String scaleType;

    @Column(name = "scale_code")
    private String scaleCode;

    @Column(name = "tariff_scale")
    private String tariffScale;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "calculation_for_number_of_days")
    private Boolean calculationForNumberOfDays;

    @Column(name = "scale_for_active_electricity")
    private Boolean scaleForActiveElectricity;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public Scales(ScalesRequest request, GridOperator gridOperator) {
        this.name = request.getName();
        this.gridOperator = gridOperator;
        this.scaleType = request.getScaleType();
        this.scaleCode = request.getScaleCode();
        this.tariffScale = request.getTariffOrScale();
        this.defaultSelection = request.getDefaultSelection();
        this.calculationForNumberOfDays = Objects.requireNonNullElse(request.getCalculationForNumberOfDays(), false);
        this.scaleForActiveElectricity = Objects.requireNonNullElse(request.getScaleForActiveElectricity(), false);
        this.status = request.getStatus();
    }
}
