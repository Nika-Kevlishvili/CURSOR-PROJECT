package bg.energo.phoenix.model.response.product;

import lombok.Builder;

@Builder
public record ProductDescriptionResponse(
        String shortDescription,
        String fullDescription
) {
}
