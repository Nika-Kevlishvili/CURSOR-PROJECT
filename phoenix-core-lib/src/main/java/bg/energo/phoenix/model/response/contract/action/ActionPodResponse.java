package bg.energo.phoenix.model.response.contract.action;

public record ActionPodResponse(Long id, String identifier) {
    // NOTE: ID here represents the ID of the PointOfDelivery entity and not the ID of the ActionPod entity.
}
