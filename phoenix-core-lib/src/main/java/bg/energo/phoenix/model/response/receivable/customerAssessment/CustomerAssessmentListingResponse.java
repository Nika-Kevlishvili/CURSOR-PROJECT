package bg.energo.phoenix.model.response.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerAssessmentListingResponse {

    private Long id;

    private String assessmentNumber;

    private AssessmentStatus assessmentStatus;

    private String type;

    private LocalDate creationDate;

    private String customer;

    private String reschedulingAgreement;

    private Assessment finalAssessment;

    private EntityStatus status;

    public CustomerAssessmentListingResponse(CustomerAssessmentListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.assessmentNumber = middleResponse.getAssessmentNumber();
        this.assessmentStatus = middleResponse.getAssessmentStatus();
        this.type = middleResponse.getType();
        this.creationDate = middleResponse.getCreationDate();
        this.customer = middleResponse.getCustomer();
        this.reschedulingAgreement = middleResponse.getReschedulingAgreement();
        this.finalAssessment = middleResponse.getFinalAssessment() ? Assessment.YES : Assessment.NO;
        this.status = middleResponse.getStatus();
    }

}
