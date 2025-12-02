package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

//@Service
//@Profile("local")
public class LocalFileService implements FileService {
    private final Path fileStorageLocation;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    @Override
    public void deleteOnPath(String localFileUrl) {

    }

    @Autowired
    public LocalFileService() {
        this.fileStorageLocation = Paths.get(System.getProperty("user.dir").concat("/upload/files"))
                .toAbsolutePath().normalize();

        try {
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(this.fileStorageLocation);
            }
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Stores a file in local storage.
     *
     * @param file       {@link MultipartFile} to be uploaded
     * @param remotePath path to which the file should be uploaded
     * @param fileName   the name to give to the remote file
     * @return full path of the uploaded file
     * @throws ClientException if any operation produces error
     */
    public String uploadFile(MultipartFile file, String remotePath, String fileName) {
        // Normalize file name
        try {
            // Check if the filename contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException(
                        "Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "upload/files/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    /**
     * Retrieves a file stored in local storage.
     *
     * @param fileUrl path of the file to be downloaded.
     * @return a {@link ByteArrayResource} representing the downloaded file.
     */
    @SneakyThrows
    @Override
    public ByteArrayResource downloadFile(String fileUrl) {
        byte[] bytes;
        if (fileUrl.contains(ftpBasePath)) {
            fileUrl = fileUrl.substring(ftpBasePath.length() + 1);
            ClassPathResource classPathResource = new ClassPathResource(fileUrl);
            bytes = classPathResource.getInputStream().readAllBytes();
        } else {
            bytes = Files.readAllBytes(Paths.get(fileUrl));
        }
        return new ByteArrayResource(bytes);
    }

    @Override
    public String uploadFile(File file, String remotePath, String fileName) {
        // Normalize file name
        try {
            // Check if the filename contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence %s".formatted(fileName));
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            File targetLocationFile = targetLocation.toAbsolutePath().toFile();
            if (!targetLocationFile.exists()) {
                Files.createDirectories(targetLocation);
            }
            Files.copy(new FileInputStream(file), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocationFile.getAbsolutePath();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file %s. Please try again!".formatted(fileName), ex);
        }
    }

    @Override
    public List<MultipartFile> fetchFilesFromFolder(String folderPath) {
        // TODO implement
        return null;
    }
}
