package bg.energo.phoenix.service.xEnergie.jobs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum XEnergieJobType {
    X_ENERGIE_DEAL_DATES_UPDATER("X_ENERGIE_DEAL_DATES_UPDATER"),
    X_ENERGIE_POINT_OF_DELIVERY_EXCEL_GENERATION("X_ENERGIE_POINT_OF_DELIVERY_EXCEL_GENERATION"),
    X_ENERGIE_SPLIT_UPDATER("X_ENERGIE_SPLIT_UPDATER"),
    X_ENERGIE_SPLIT_CREATION_COMMITMENT("X_ENERGIE_SPLIT_CREATION_COMMITMENT"),
    X_ENERGIE_DEAL_CREATION("X_ENERGIE_DEAL_CREATION");

    private final String jobExceptionMessageHeader;
}
