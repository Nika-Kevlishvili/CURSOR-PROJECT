package bg.energo.phoenix.model.response.termsGroup;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TermsGroupVersionsResponse {
    Long versionId;
    String name;
    LocalDateTime creationDate;  // This is not a createDate but a startDate of the version
}
