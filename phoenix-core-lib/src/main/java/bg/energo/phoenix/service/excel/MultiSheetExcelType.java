package bg.energo.phoenix.service.excel;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum MultiSheetExcelType {
    MASS_IMPORT_ERROR_REPORT("mass-import-error-report"),
    BILLING_RUN_ERROR_REPORT("billing-run-error-report");

    private final String value;

    public static MultiSheetExcelType fromValue(String value) {
        return Arrays
                .stream(MultiSheetExcelType.values())
                .filter(v -> v.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new DomainEntityNotFoundException("MultiSheetExcelType " + value + "not found"));
    }
}
