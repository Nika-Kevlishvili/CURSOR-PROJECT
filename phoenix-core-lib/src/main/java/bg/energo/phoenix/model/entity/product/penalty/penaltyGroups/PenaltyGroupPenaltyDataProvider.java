package bg.energo.phoenix.model.entity.product.penalty.penaltyGroups;

public record PenaltyGroupPenaltyDataProvider(Long id, Long penaltyId, String penaltyName) implements PenaltyGroupPenaltyQueryResponse {
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Long getPenaltyId() {
        return penaltyId;
    }

    @Override
    public String getPenaltyName() {
        return penaltyName;
    }
}
