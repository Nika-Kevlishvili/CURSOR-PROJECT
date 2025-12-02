package bg.energo.phoenix.model.request.receivable.customerAssessment;

import lombok.Getter;

public enum CustomerAssessmentSearchByEnums {
    NUMBER("ASSESSMENT_NUMBER"),
    CUSTOMER("CUSTOMER"),
    RESCHEDULING_AGREEMENT("RESCHEDULING_AGREEMENT"),
    ALL("ALL");

    @Getter
    private final String value;

    CustomerAssessmentSearchByEnums(String value) {
        this.value = value;
    }
}
