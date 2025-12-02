package bg.energo.phoenix.model.response.product;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class ProductVersionShortResponse {
    private Long id; // this is the ID of the version
    private String name;

    public ProductVersionShortResponse(Long id,
                                       String name,
                                       Long version,
                                       String customerIdentifier) {
        this.id = id;
        if (StringUtils.isNotEmpty(customerIdentifier)) {
            this.name = "%s".formatted(name);
        } else {
            this.name = "%s (Version %s)".formatted(name, version);
        }
    }

}
