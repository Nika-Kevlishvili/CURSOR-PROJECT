package bg.energo.phoenix.model.response.receivable.customerAssessment;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CustomerAssessmentResponse {
    private Long id;

    private String number;

    private LocalDate creationDate;

    private AssessmentStatus assessmentStatus;

    private Assessment finalAssessment;

    private ShortResponse customerAssessmentType;

    private List<ShortResponse> additionalConditions;

    private List<CustomerAssessmentParametersResponse> parameters;

    private List<CustomerAssessmentCommentHistory> comments;

    private ShortResponse customer;

    private List<TaskShortResponse> tasks;

    private List<FileWithStatusesResponse> files;

    private ShortResponse rescheduling;

    private EntityStatus status;

    public CustomerAssessmentResponse(CustomerAssessment customerAssessment) {
        this.id = customerAssessment.getId();
        this.number = customerAssessment.getAssessmentNumber();
        this.creationDate = customerAssessment.getCreateDate().toLocalDate();
        this.assessmentStatus = customerAssessment.getAssessmentStatus();
        this.finalAssessment = customerAssessment.getFinalAssessment() ? Assessment.YES : Assessment.NO;
        this.status = customerAssessment.getStatus();
    }
}
