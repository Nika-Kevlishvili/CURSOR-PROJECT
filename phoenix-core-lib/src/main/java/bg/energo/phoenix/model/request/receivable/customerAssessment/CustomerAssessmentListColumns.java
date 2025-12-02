package bg.energo.phoenix.model.request.receivable.customerAssessment;

import lombok.Getter;

public enum CustomerAssessmentListColumns {
    NUMBER("id"),
    STATUS("assessmentStatus"),
    TYPE("type"),
    CREATION_DATE("creationDate"),
    CUSTOMER("customer"),
    RESCHEDULING_AGREEMENT("reschedulingAgreement"),
    FINAL_ASSESSMENT("finalAssessment");

    @Getter
    private final String value;

    CustomerAssessmentListColumns(String collectionChannelColumn) {
        this.value = collectionChannelColumn;
    }
}
