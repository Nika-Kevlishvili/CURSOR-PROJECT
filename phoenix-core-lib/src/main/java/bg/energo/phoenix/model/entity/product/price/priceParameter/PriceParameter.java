package bg.energo.phoenix.model.entity.product.price.priceParameter;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.price.priceParameter.PriceParameterStatus;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
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

@Table(name = "price_parameters", schema = "prices")
public class PriceParameter extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "price_parameters_id_seq",
            sequenceName = "prices.price_parameters_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "price_parameters_id_seq"
    )
    private Long id;

    @Column(name = "period_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PeriodType periodType;

    @Column(name = "time_zone")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TimeZone timeZone;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PriceParameterStatus status;

    @Column(name = "last_price_parameter_detail_id")
    private Long lastPriceParameterDetailId;

}
