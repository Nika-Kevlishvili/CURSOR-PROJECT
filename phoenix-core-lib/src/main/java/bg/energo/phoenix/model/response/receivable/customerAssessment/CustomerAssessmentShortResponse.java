package bg.energo.phoenix.model.response.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAssessmentShortResponse {

    private Long id;

    private String assessmentNumber;

    private LocalDate creationDate;

    private AssessmentStatus assessmentStatus;

    private Long customerId;

    private Assessment finalAssessment;

    private Long customerAssessmentTypeId;

    private EntityStatus status;

    public CustomerAssessmentShortResponse(CustomerAssessment customerAssessment) {
        this.id = customerAssessment.getId();
        this.assessmentNumber = customerAssessment.getAssessmentNumber();
        this.creationDate = customerAssessment.getCreateDate().toLocalDate();
        this.assessmentStatus = customerAssessment.getAssessmentStatus();
        this.customerId = customerAssessment.getCustomerId();
        this.finalAssessment = customerAssessment.getFinalAssessment() ? Assessment.YES : Assessment.NO;
        this.customerAssessmentTypeId = customerAssessment.getCustomerAssessmentTypeId();
        this.status = customerAssessment.getStatus();
    }
}
