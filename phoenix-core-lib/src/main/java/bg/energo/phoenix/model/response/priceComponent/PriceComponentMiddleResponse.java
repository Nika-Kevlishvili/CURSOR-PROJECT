package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;

import java.time.LocalDateTime;

public interface PriceComponentMiddleResponse {
    Long getId();

    String getName();

    String getPrice();

    String getValue();

    String getNumber();

    String getConditions();

    String getAvailable();

    String getFormula();

    LocalDateTime getCdate();

    PriceComponentStatus getStatus();


}
