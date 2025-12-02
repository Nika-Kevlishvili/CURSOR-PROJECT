package bg.energo.phoenix.service.billing.companyDetails;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyLogos;
import bg.energo.phoenix.model.response.billing.CompanyDetailFileResponse;
import bg.energo.phoenix.model.response.billing.CompanyFileContent;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompanyDetailFileService {

    private final CompanyLogoRepository companyLogoRepository;
    private final FileService fileService;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;


    /**
     * Uploads a company detail file to the server and saves it in the database.
     *
     * @param file The MultipartFile representing the file to be uploaded.
     * @return A CompanyDetailFileResponse object containing the details of the uploaded file.
     * @throws ClientException if the file name is null.
     */
//    @CacheEvict(value = "companyDetailsForTemplateCache", allEntries = true)
    public CompanyDetailFileResponse uploadCompanyDetailFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null", ErrorCode.APPLICATION_ERROR);
        }
        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "company_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);
        CompanyLogos companyLogos = new CompanyLogos();
        companyLogos.setFileUrl(url);
        companyLogos.setName(fileName);
        companyLogos.setStatus(EntityStatus.ACTIVE);
        CompanyLogos savedEntity = companyLogoRepository.save(companyLogos);
        return new CompanyDetailFileResponse(savedEntity);
    }

    /**
     * This method cleans up the company file data by setting the status of all company logos to "DELETED".
     */
    public void cleanupCompanyFileData() {
        List<CompanyLogos> companyLogos = companyLogoRepository.findActiveByCompanyDetailIdNull();
        companyLogos.forEach(companyFile -> companyFile.setStatus(EntityStatus.DELETED));
        companyLogoRepository.saveAll(companyLogos);
    }

    /**
     * Downloads a file with the given ID.
     *
     * @param id the ID of the file to be downloaded
     * @return the downloaded file content as a CompanyFileContent object
     * @throws DomainEntityNotFoundException if the file with the given ID is not found
     */
    public CompanyFileContent downloadFile(Long id) {
        CompanyLogos companyFile = companyLogoRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-company file with ID %s not found;".formatted(id)));
        return new CompanyFileContent(companyFile.getName(), fileService.downloadFile(companyFile.getFileUrl()).getByteArray());
    }
}
