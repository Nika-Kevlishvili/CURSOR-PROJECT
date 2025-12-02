package bg.energo.phoenix.model.response.nomenclature.billing;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;

public record RiskAssessmentResponse(
        Long id,
        String name,
        Long orderingId,
        boolean defaultSelection,
        NomenclatureItemStatus status
) {
}
