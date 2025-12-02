package bg.energo.phoenix.util.epb;

import bg.energo.phoenix.exception.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

@Slf4j
public class EPBDocxUtils {

    private static final String DOCX_FILE_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    public static void validateFileFormat(MultipartFile file) {
        if (!hasDocxFormat(file)) {
            log.error("File has invalid format");
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    private static boolean hasDocxFormat(MultipartFile file) {
        return Objects.equals(file.getContentType(), DOCX_FILE_TYPE);
    }

}

