package bg.energo.phoenix.model.enums.product.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CopyDomainWithVersionBasedRequestFilter {
    INDIVIDUAL_PRODUCT("INDIVIDUAL_PRODUCT"),
    INDIVIDUAL_SERVICE("INDIVIDUAL_SERVICE");

    private final String value;
}
