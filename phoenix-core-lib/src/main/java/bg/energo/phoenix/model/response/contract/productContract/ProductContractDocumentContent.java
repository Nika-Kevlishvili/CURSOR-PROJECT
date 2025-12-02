package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.util.epb.EPBFinalFields;

public record ProductContractDocumentContent(String fileName, byte[] content) {
    public ProductContractDocumentContent(String fileName, byte[] content) {
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
