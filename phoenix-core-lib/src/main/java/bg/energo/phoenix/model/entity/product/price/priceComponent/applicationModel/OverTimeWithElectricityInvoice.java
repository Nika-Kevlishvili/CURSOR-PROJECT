package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeWithElectricityPeriodType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeWithElectricityType;
import bg.energo.phoenix.model.request.product.price.aplicationModel.OverTimeWithElectricityInvoiceRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "am_over_time_with_electricity_invoices", schema = "price_component")
public class OverTimeWithElectricityInvoice extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "application_model_over_time_one_time_id_seq",
            sequenceName = "price_component.application_model_over_time_one_time_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "application_model_over_time_one_time_id_seq"
    )
    private Long id;
    @JoinColumn(name = "application_model_id")
    private Long applicationModelId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OverTimeWithElectricityType type;

    @Column(name = "period_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OverTimeWithElectricityPeriodType periodType;

    public OverTimeWithElectricityInvoice(ApplicationModel applicationModelId, OverTimeWithElectricityInvoiceRequest request) {
        this.applicationModelId = applicationModelId.getId();
        this.type = Objects.requireNonNullElse(request.getWithEveryInvoice(), false) ? OverTimeWithElectricityType.WITH_EVERY_INVOICE : OverTimeWithElectricityType.AT_MOST_ONCE;
        this.periodType = Objects.requireNonNullElse(request.getAtMostOncePer(), false) ? request.getOverTimeWithElectricityPeriodType() : null;
        this.status = EntityStatus.ACTIVE;
    }
}
