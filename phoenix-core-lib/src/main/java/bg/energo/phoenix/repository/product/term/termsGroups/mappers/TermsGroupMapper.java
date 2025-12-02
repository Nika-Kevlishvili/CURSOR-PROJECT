package bg.energo.phoenix.repository.product.term.termsGroups.mappers;

import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupVersionsResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupViewResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TermsGroupMapper {

    public TermsGroupViewResponse termsGroupViewResponseMap(TermsGroups termsGroups, TermGroupDetails termGroupDetails, Terms terms, List<TermsGroupVersionsResponse> versionsList) {
        TermsGroupViewResponse build = TermsGroupViewResponse.builder()
                .termsGroupId(termsGroups.getId())
                .termGroupStatus(termsGroups.getStatus())
                .termsGroupDetailsId(termGroupDetails.getId())
                .termsGroupDetailsName(termGroupDetails.getName())
                .termsGroupDetailsGroupId(termGroupDetails.getGroupId())
                .termsGroupDetailsVersionId(termGroupDetails.getVersionId())
                .createDate(termGroupDetails.getCreateDate())
                .versions(versionsList).build();
        if (terms != null) {
            build.setTermId(terms.getId());
            build.setTermName(terms.getName());
        }
        return build;
    }

    public TermsGroupVersionsResponse termsVersionsMap(TermGroupDetails termGroupDetails) {
        return TermsGroupVersionsResponse.builder()
                .versionId(termGroupDetails.getVersionId())
                .name(termGroupDetails.getName())
                .creationDate(termGroupDetails.getStartDate()) // version start date
                .build();
    }
}
