package bg.energo.phoenix.model.response.receivable.customerAssessment;

public interface CustomerAssessmentDraftResponse {

    String getConditions();
    String getValue();
    Long getCustomerAssessmentCriteriaId();
    Boolean getIsValue();
    Integer getValueFrom();
    Integer getValueTo();
    String getCustomerType();
    Boolean getAssessment();
    Boolean getFinalAssessment();
    Long getCustomerAssessmentParametersId();
    Long getCustomerAssessmentId();

}
