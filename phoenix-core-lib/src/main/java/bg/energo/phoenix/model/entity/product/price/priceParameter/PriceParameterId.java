package bg.energo.phoenix.model.entity.product.price.priceParameter;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class PriceParameterId implements Serializable {



    private Long id;

    private Long versionId;


}
