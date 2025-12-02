package bg.energo.phoenix.model.enums.pod.billingByProfile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingByProfileTableColumn {
    BBP_ID("id"),
    POD_IDENTIFIER("podIdentifier"),
    PERIOD_FROM("periodFrom"),
    PERIOD_TO("periodTo"),
    PROFILE_NAME("profileName"),
    PERIOD_TYPE("periodType"),
    INVOICED("invoiced"),
    BBP_DATE_OF_CREATION("createDateSort");

    private final String value;
}
