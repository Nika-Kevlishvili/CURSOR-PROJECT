package bg.energo.phoenix.model.response.receivable.customerAssessment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAssessmentCommentHistory {
    private String commenter;
    private LocalDateTime createDate;
    private String comment;
}
