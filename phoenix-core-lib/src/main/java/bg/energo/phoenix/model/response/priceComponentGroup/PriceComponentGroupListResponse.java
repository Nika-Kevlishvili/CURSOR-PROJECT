package bg.energo.phoenix.model.response.priceComponentGroup;

import java.time.LocalDateTime;

public interface PriceComponentGroupListResponse {

    Long getGroupId();

    String getName();

    Long getNumberOfPriceComponents();

    LocalDateTime getDateOfCreation();

    String getStatus();

}
