package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentType;

public record CustomerAssessmentTypeShortResponse(
        Long id,
        String name
) {

    public CustomerAssessmentTypeShortResponse(CustomerAssessmentTypeMiddleResponse middleResponse) {
        this(middleResponse.getId(), middleResponse.getName());
    }

    public CustomerAssessmentTypeShortResponse(CustomerAssessmentType customerAssessmentType) {
        this(customerAssessmentType.getId(), customerAssessmentType.getName());
    }
}
