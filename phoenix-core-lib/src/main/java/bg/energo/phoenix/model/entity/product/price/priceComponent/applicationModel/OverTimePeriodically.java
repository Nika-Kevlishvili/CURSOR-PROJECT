package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimePeriodicallyStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.Periodicity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "am_over_time_periodically", schema = "price_component")
public class OverTimePeriodically extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "am_over_time_periodically_id_seq",
            sequenceName = "price_component.am_over_time_periodically_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "am_over_time_periodically_id_seq"
    )
    private Long id;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OverTimePeriodicallyStatus status;
    @Column(name="year_round")
    private Boolean yearRound;
    @Column(name = "rrule_formula")
    private String rruleFormula;
    @ManyToOne
    @JoinColumn(name = "application_model_id")
    private ApplicationModel applicationModel;

    @Column(name = "periodicity")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Periodicity periodicity;
}
