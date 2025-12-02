package bg.energo.phoenix.model.entity.nomenclature.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "bnb_base_interest_rates", schema = "nomenclature")
public class BaseInterestRate extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "bnb_base_interest_rates_id_seq",
            sequenceName = "nomenclature.bnb_base_interest_rates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "bnb_base_interest_rates_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "percentage_rate")
    private BigDecimal percentageRate;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

}
