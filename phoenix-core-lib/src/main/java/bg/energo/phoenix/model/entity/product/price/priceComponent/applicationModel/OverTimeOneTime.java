package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeOneTimeStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeOneTimeType;
import bg.energo.phoenix.model.request.product.price.aplicationModel.OverTimeOneTimeRequest;
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

@Table(name = "am_over_time_one_times", schema = "price_component")
public class OverTimeOneTime extends BaseEntity {
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
    @ManyToOne
    @JoinColumn(name = "application_model_id")
    private ApplicationModel applicationModel;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OverTimeOneTimeType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OverTimeOneTimeStatus status;

    public OverTimeOneTime(ApplicationModel applicationModel, OverTimeOneTimeRequest request) {
        this.applicationModel = applicationModel;
        this.type = request.getType();
        this.status = OverTimeOneTimeStatus.ACTIVE;
    }
}
