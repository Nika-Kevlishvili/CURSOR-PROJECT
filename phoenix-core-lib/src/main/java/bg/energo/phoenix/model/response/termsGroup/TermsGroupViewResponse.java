package bg.energo.phoenix.model.response.termsGroup;


import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TermsGroupViewResponse {
    //TermsGroup
    private Long termsGroupId;
    private TermGroupStatus termGroupStatus;
    //TermsGroupDetails
    private Long termsGroupDetailsId;
    private String termsGroupDetailsName;
    private Long termsGroupDetailsGroupId;
    private Long termsGroupDetailsVersionId;
    private LocalDateTime createDate;
    //Term Info
    private Long termId;
    private String termName;
    //versions
    List<TermsGroupVersionsResponse> versions;
    private Boolean isLocked;
}
