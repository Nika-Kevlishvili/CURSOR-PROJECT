package bg.energo.phoenix.model.response.termsGroup;

import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import lombok.Data;

@Data
public class TermsGroupsShortResponse {
    private Long id;
    private Long versionId;
    private String name;

    public TermsGroupsShortResponse(TermsGroups termsGroups, TermGroupDetails termGroupDetails) {
        this.id = termsGroups.getId();
        this.versionId = termGroupDetails.getVersionId();
        this.name = "%s (%s)".formatted(termGroupDetails.getName(), termsGroups.getId());
    }
}
