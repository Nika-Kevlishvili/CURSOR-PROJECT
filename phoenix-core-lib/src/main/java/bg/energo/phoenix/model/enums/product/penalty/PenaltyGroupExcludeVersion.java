package bg.energo.phoenix.model.enums.product.penalty;

import lombok.Getter;

public enum PenaltyGroupExcludeVersion {
    NONE(null),
    PAST_VERSION("PASTVERSION"),
    RESPECT_VERSION("RESPECTVERSION"),
    PAST_AND_RESPECT_VERSION("PASTANDRESPECTVERSION");
    @Getter
    private final String value;

    PenaltyGroupExcludeVersion(String value) {
        this.value = value;
    }

    public static PenaltyGroupExcludeVersion getExcludeVersionFromCheckBoxes(boolean excludeOldVersions, boolean excludeFutureVersions) {
        if (excludeOldVersions && excludeFutureVersions) {
            return PAST_AND_RESPECT_VERSION;
        } else if (excludeOldVersions) {
            return PAST_VERSION;
        } else if (excludeFutureVersions) {
            return RESPECT_VERSION;
        } else {
            return NONE;
        }
    }
}
