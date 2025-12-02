package bg.energo.phoenix.model.enums.product;

import lombok.Getter;

@Getter
public enum ExcludeVersions {
    NONE(null),
    OLD_VERSION("OLDVERSION"),
    FUTURE_VERSION("FUTUREVERSION"),
    OLD_AND_FUTURE_VERSION("OLDANDFUTUREVERSION");
    private final String value;

    ExcludeVersions(String value) {
        this.value = value;
    }

    public static ExcludeVersions getExcludeVersionFromCheckBoxes(boolean excludeOldVersions, boolean excludeFutureVersions) {
        if (excludeOldVersions && excludeFutureVersions) {
            return OLD_AND_FUTURE_VERSION;
        } else if (excludeOldVersions) {
            return OLD_VERSION;
        } else if (excludeFutureVersions) {
            return FUTURE_VERSION;
        } else {
            return NONE;
        }
    }

}
