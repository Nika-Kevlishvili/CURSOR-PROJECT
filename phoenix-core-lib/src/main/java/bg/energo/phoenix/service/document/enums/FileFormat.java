package bg.energo.phoenix.service.document.enums;

import bg.energo.phoenix.model.enums.template.ContractTemplateFileFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileFormat {
    DOCX("docx"),
    PDF("pdf"),
    XLSX("xlsx");

    public final String suffix;

    public static FileFormat fromContractTemplateFileFormat(ContractTemplateFileFormat contractTemplateFileFormat) {
        if (contractTemplateFileFormat == null) {
            throw new IllegalArgumentException("Contract Template File Format can not be null;");
        }

        switch (contractTemplateFileFormat) {
            case DOCX -> {
                return DOCX;
            }
            case PDF -> {
                return PDF;
            }
            case XLSX -> {
                return XLSX;
            }
            default -> throw new IllegalArgumentException("Cannot define File Format");
        }
    }
}
