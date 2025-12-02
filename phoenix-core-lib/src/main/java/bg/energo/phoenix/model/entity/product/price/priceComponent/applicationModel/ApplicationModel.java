package bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationLevel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
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

@Table(name = "application_models", schema = "price_component")
public class ApplicationModel extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "application_models_ids_seq",
            sequenceName = "price_component.application_models_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "application_models_ids_seq"
    )
    private Long id;

    @Column(name = "application_model_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelType applicationModelType;

    @Column(name = "application_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationType applicationType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationModelStatus status;

    @OneToOne
    @JoinColumn(name = "price_component_id")
    private PriceComponent priceComponent;

    @Column(name = "application_level")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ApplicationLevel applicationLevel;
}
