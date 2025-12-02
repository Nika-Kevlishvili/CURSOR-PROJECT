package bg.energo.phoenix.model.request.receivable.customerAssessment;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class CustomerAssessmentListingRequest {

    @Size(min = 1, message = "prompt-Prompt length must be 1 or more")
    private String prompt;

    private CustomerAssessmentListColumns sortBy;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private CustomerAssessmentSearchByEnums searchBy;

    private List<AssessmentStatus> assessmentStatuses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateFrom", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "createDateTo", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate createDateTo;

    private List<Assessment> finalAssessment;

    private List<Long> assessmentTypeIds;

    private Sort.Direction direction;

}
