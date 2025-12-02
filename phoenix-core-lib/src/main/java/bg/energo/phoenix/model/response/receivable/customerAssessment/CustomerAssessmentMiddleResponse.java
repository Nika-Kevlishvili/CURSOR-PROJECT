package bg.energo.phoenix.model.response.receivable.customerAssessment;

import java.math.BigDecimal;

public interface CustomerAssessmentMiddleResponse {

    String getConditions();
    String getValue();
    Long getCustomerAssessmentCriteriaId();
    Boolean getIsValue();
    BigDecimal getValueFrom();
    BigDecimal getValueTo();
    String getCustomerType();
    Boolean getAssessment();
    Boolean getFinalAssessment();

}
