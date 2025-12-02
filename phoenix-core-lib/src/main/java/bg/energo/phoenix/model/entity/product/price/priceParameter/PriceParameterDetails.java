package bg.energo.phoenix.model.entity.product.price.priceParameter;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "price_parameter_details", schema = "prices")
public class PriceParameterDetails extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "price_parameter_details_id_seq",
            sequenceName = "prices.price_parameter_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "price_parameter_details_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "price_parameter_id")
    private Long priceParameterId;

    public PriceParameterDetails(String name, Long versionId, Long priceParameterId) {
        this.name = name;
        this.versionId = versionId;
        this.priceParameterId = priceParameterId;
    }
}
