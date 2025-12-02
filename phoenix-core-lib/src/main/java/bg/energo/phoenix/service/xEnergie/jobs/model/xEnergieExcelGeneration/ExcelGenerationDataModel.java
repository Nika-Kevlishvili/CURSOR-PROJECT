package bg.energo.phoenix.service.xEnergie.jobs.model.xEnergieExcelGeneration;

import java.time.LocalDate;

public record ExcelGenerationDataModel(
        String pointOfDeliveryIdentifier,
        String pointOfDeliveryAdditionalIdentifier,
        String pointOfDeliveryIdentifierForEANorEIC,
        LocalDate activationDate,
        LocalDate deactivationDate,
        String codeForXEnergie,
        String gridOperatorCode,
        Long customerNumber,
        String gridOperatorAdditionalCode, // same as code for xEnergie from Grid Operator Nomenclature
        String dealNumber,
        Boolean ownedByEnergoPro
) {
}
