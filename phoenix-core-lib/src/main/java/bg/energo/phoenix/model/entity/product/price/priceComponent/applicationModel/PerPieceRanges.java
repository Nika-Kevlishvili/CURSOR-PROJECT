package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PerPieceStatus;
import bg.energo.phoenix.model.request.product.price.aplicationModel.SettlementPeriodRange;
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

@Table(name = "am_per_piece_ranges", schema = "price_component")
public class PerPieceRanges extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "application_model_per_piece_ranges_id_seq",
            sequenceName = "price_component.application_model_per_piece_ranges_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "application_model_per_piece_ranges_id_seq"
    )
    private Long id;
    @ManyToOne
    @JoinColumn(name = "application_model_id")
    private ApplicationModel applicationModel;

    @Column(name = "value_from")
    private Integer valueFrom;
    @Column(name = "value_to")
    private Integer valueTo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PerPieceStatus status;

    public PerPieceRanges(ApplicationModel applicationModel, SettlementPeriodRange range) {
        this.applicationModel = applicationModel;
        this.valueFrom = range.getFrom();
        this.valueTo = range.getTo();
        this.status = PerPieceStatus.ACTIVE;
    }
}
