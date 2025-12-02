package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.entity.product.price.priceComponent.ProfileForBalancing;

public record ProfileForBalancingShortResponse(Long id, String name) {
    public ProfileForBalancingShortResponse(ProfileForBalancing profileForBalancing) {
        this(profileForBalancing.getId(), profileForBalancing.getName());
    }
}
