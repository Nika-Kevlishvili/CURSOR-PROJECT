package bg.energo.phoenix.model.response.nomenclature.receivable;

import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;

public record GroundForObjectionWithdrawalToChangeOfACbgResponse(
        Long id,
        String name,
        Long orderingId,
        boolean defaultSelection,
        NomenclatureItemStatus status
) {
}
