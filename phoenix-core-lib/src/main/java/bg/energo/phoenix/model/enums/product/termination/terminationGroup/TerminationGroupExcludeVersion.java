package bg.energo.phoenix.model.enums.product.termination.terminationGroup;

import lombok.Getter;

public enum TerminationGroupExcludeVersion {
    NONE(null),
    PAST_VERSION("PASTVERSION"),
    RESPECT_VERSION("RESPECTVERSION"),
    PAST_AND_RESPECT_VERSION("PASTANDRESPECTVERSION");
    @Getter
    private final String value;

    TerminationGroupExcludeVersion(String value) {
        this.value = value;
    }

    public static TerminationGroupExcludeVersion getExcludeVersionFromCheckBoxes(boolean excludeOldVersions, boolean excludeFutureVersions) {
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
