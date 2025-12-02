package bg.energo.phoenix.process.model.response;

import bg.energo.phoenix.util.epb.EPBFinalFields;

public record ProcessFileContent(String fileName, byte[] content) {
    public ProcessFileContent(String fileName, byte[] content) {
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
