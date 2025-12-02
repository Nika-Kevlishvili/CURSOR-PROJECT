package bg.energo.phoenix.model.response.receivable.customerAssessment;

public interface CustomerAssessmentFinalStatusResponse {

    String getConditions();
    String getValue();
    Boolean getAssessment();
    Boolean getFinalAssessment();
    Long getCustomerAssessmentParametersId();
    Long getCustomerAssessmentId();
    Long getCustomerAssessmentCriteriaId();
}
