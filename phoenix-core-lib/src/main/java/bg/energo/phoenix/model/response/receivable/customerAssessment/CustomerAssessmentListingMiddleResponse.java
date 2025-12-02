package bg.energo.phoenix.model.response.receivable.customerAssessment;


import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;

import java.time.LocalDate;

public interface CustomerAssessmentListingMiddleResponse {

    Long getId();

    String getAssessmentNumber();

    AssessmentStatus getAssessmentStatus();

    String getType();

    LocalDate getCreationDate();

    String getCustomer();

    String getReschedulingAgreement();

    boolean getFinalAssessment();

    EntityStatus getStatus();

}
