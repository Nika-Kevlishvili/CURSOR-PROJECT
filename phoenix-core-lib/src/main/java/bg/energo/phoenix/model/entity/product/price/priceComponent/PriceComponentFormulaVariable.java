package bg.energo.phoenix.model.entity.product.price.priceComponent;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "price_component_formula_variables", schema = "price_component")
public class PriceComponentFormulaVariable extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "price_component_formula_variables_id_seq",
            sequenceName = "price_component.price_component_formula_variables_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "price_component_formula_variables_id_seq"
    )
    private Long id;

    @Column(name = "formula_variable")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PriceComponentMathVariableName variable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_component_id", referencedColumnName = "id")
    private PriceComponent priceComponent;

    @Column(name = "description")
    private String description;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "value_from")
    private BigDecimal valueFrom;

    @Column(name = "value_to")
    private BigDecimal valueTo;

    @ManyToOne
    @JoinColumn(name = "profile_for_balancing_id", referencedColumnName = "id")
    private ProfileForBalancing profileForBalancing;

    @Override
    public String toString() {
        return "PriceComponentFormulaVariable{" +
               "id=" + id +
               ", variable=" + variable +
               ", priceComponentId=" + priceComponent.getId() +
               ", description='" + description + '\'' +
               ", value=" + value +
               ", valueFrom=" + valueFrom +
               ", valueTo=" + valueTo +
               '}';
    }
}
