package bg.energo.phoenix.service.document.ftpService;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface FileService {

    /**
     * Uploads a file
     *
     * @param file       file to be uploaded
     * @param remotePath path to which the file should be uploaded
     * @param fileName   the name to give to the remote file
     * @return full path of the uploaded file
     */
    String uploadFile(MultipartFile file, String remotePath, String fileName);

    /**
     * Downloads a file as a {@link ByteArrayResource}
     *
     * @param fileUrl path of the file to be downloaded
     * @return {@link ByteArrayResource} representing the downloaded file
     */
    ByteArrayResource downloadFile(String fileUrl);

    /**
     * Uploads a file to a remote location.
     *
     * @param file       the file to be uploaded
     * @param remotePath the path to which the file should be uploaded
     * @param fileName   the name to give to the remote file
     * @return the full path of the uploaded file
     */
    String uploadFile(File file, String remotePath, String fileName);

    List<MultipartFile> fetchFilesFromFolder(String folderPath);

    void deleteOnPath(String localFileUrl);
}
