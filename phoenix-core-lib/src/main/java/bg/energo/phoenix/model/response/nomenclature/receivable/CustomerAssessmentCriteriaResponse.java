package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentCriteria;
import bg.energo.phoenix.model.enums.nomenclature.CustomerAssessmentCriteriaType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;

import java.math.BigDecimal;

public record CustomerAssessmentCriteriaResponse(
        Long id,
        String name,
        BigDecimal valueFrom,
        BigDecimal valueTo,
        Boolean value,
        CustomerAssessmentCriteriaType criteriaType,
        Boolean defaultSelection,
        Boolean isHardCoded,
        NomenclatureItemStatus status,
        String systemUserId
) {
    public CustomerAssessmentCriteriaResponse(CustomerAssessmentCriteria customerAssessmentCriteria) {
        this(
                customerAssessmentCriteria.getId(),
                customerAssessmentCriteria.getName(),
                customerAssessmentCriteria.getValueFrom(),
                customerAssessmentCriteria.getValueTo(),
                customerAssessmentCriteria.getValue(),
                customerAssessmentCriteria.getCriteriaType(),
                customerAssessmentCriteria.isDefaultSelection(),
                customerAssessmentCriteria.isHardCoded(),
                customerAssessmentCriteria.getStatus(),
                customerAssessmentCriteria.getSystemUserId()
        );
    }
}
