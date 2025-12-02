package bg.energo.phoenix.model.response.product;

import bg.energo.phoenix.model.entity.product.product.ProductForBalancing;

public record ProductForBalancingShortResponse(Long id, String name) {
    public ProductForBalancingShortResponse(ProductForBalancing productForBalancing) {
        this(productForBalancing.getId(), productForBalancing.getName());
    }
}
