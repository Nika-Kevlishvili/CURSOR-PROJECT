package bg.energo.phoenix.model.request.product.price.aplicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.OverTimeOneTimeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OverTimeOneTimeRequest {
    @NotNull(message = "applicationModelRequest.overTimeOneTimeRequest.type-type can not be null;")
    private OverTimeOneTimeType type;
}
