package bg.energo.phoenix.model.response.receivable.latePaymentFine;

import bg.energo.phoenix.util.epb.EPBFinalFields;

public record LatePaymentFineFileContent(String fileName, byte[] content) {
    public LatePaymentFineFileContent(String fileName, byte[] content) {
        String formattedFileName;
        try {
            formattedFileName = fileName.substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            formattedFileName = fileName;
        }
        this.fileName = formattedFileName;
        this.content = content;
    }
}
