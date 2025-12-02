package bg.energo.phoenix.model.response.service;

import lombok.Builder;

@Builder
public record ServiceDescriptionResponse(
        String shortDescription,
        String fullDescription
) {
}
