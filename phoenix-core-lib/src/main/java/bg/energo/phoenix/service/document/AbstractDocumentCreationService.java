package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyLogos;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import bg.energo.phoenix.util.transliteration.BulgarianTransliterationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDocumentCreationService {

    protected final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    protected final ContractTemplateRepository contractTemplateRepository;
    protected final DocumentGenerationService documentGenerationService;
    protected final DocumentGenerationUtil documentGenerationUtil;
    protected final DocumentsRepository documentsRepository;
    protected final CompanyDetailRepository companyDetailRepository;
    protected final CompanyLogoRepository companyLogoRepository;
    protected final SignerChainManager signerChainManager;
    protected final FileService fileService;

    @Value("${ftp.server.base.path}")
    protected String ftpBasePath;

    /**
     * Fetches the company detailed information model for the given billing date.
     * This method retrieves the company detailed information by querying the repository with the provided
     * billing date. The retrieved information corresponds to the details of the company relevant to the given date.
     *
     * @param billingDate The billing date used to fetch the respective company detailed information.
     * @return The {@link CompanyDetailedInformationModel} containing the company details for the given billing date.
     */
    protected CompanyDetailedInformationModel fetchCompanyDetailedInformationModel(LocalDate billingDate) {
        log.debug("Starting fetching respective company detailed information");
        return companyDetailRepository.getCompanyDetailedInformation(billingDate);
    }

    /**
     * Fetches the company logo content as a byte array for the given company detailed information.
     * This method retrieves the company logo by searching for the first active logo associated with
     * the company detailed information. If a logo is found, it downloads the logo content and returns
     * it as a byte array. If no logo is found or an error occurs during the process, it returns null.
     *
     * @param companyDetailedInformation The company detailed information used to identify the company
     *                                   for which the logo is being fetched.
     * @return A byte array representing the company logo content, or null if no logo is found or an error occurs.
     */
    protected byte[] fetchCompanyLogoContent(CompanyDetailedInformationModel companyDetailedInformation) {
        byte[] companyLogoContent = null;
        try {
            Optional<CompanyLogos> companyLogoOptional = companyLogoRepository.findFirstByCompanyDetailIdAndStatus(
                    companyDetailedInformation.getCompanyDetailId(),
                    EntityStatus.ACTIVE
            );
            if (companyLogoOptional.isPresent()) {
                CompanyLogos companyLogo = companyLogoOptional.get();

                companyLogoContent = fileService.downloadFile(companyLogo.getFileUrl()).getContentAsByteArray();
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to download company logo");
        }
        return companyLogoContent;
    }

    /**
     * Builds and populates the provided model with shared company information.
     * This method fetches the company detailed information model and the company logo content
     * based on the current date. It then populates the provided model with this information
     * by setting the company logo and filling in the company details.
     *
     * @param <Model> The type of model that extends {@link CompanyDetailedInformationModelImpl}.
     * @param model   The model to be populated with shared company information.
     * @return The populated model with company logo and detailed information.
     */
    protected <Model extends CompanyDetailedInformationModelImpl> Model buildModelWithSharedInfo(Model model) {
        CompanyDetailedInformationModel companyDetailedInformationModel = fetchCompanyDetailedInformationModel(LocalDate.now());
        model.CompanyLogo = fetchCompanyLogoContent(companyDetailedInformationModel);
        model.fillCompanyDetailedInformation(companyDetailedInformationModel);
        return model;
    }

    /**
     * Retrieves the contract template associated with the given template ID.
     * <p>
     * This method attempts to find the contract template by its ID. If the template is not found,
     * it throws a {@link DomainEntityNotFoundException}.
     *
     * @param templateId The ID of the contract template to be retrieved.
     * @return The {@link ContractTemplate} associated with the specified ID.
     * @throws DomainEntityNotFoundException If no contract template is found for the given ID.
     */
    protected ContractTemplate getContractTemplate(Long templateId) {
        return contractTemplateRepository
                .findById(templateId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template with id: %s".formatted(templateId))
                );
    }

    /**
     * Retrieves the contract template detail associated with the given template detail ID.
     * This method attempts to find the contract template detail by its ID. If the template detail is not found,
     * it throws a {@link DomainEntityNotFoundException}.
     *
     * @param contractTemplateDetailId The ID of the contract template detail to be retrieved.
     * @return The {@link ContractTemplateDetail} associated with the specified ID.
     * @throws DomainEntityNotFoundException If no contract template detail is found for the given ID.
     */
    protected ContractTemplateDetail getContractTemplateDetails(Long contractTemplateDetailId) {
        return contractTemplateDetailsRepository
                .findById(contractTemplateDetailId)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template details with id: %s".formatted(contractTemplateDetailId))
                );
    }

    /**
     * Retrieves the last contract template detail for the given contract template ID.
     * This method first attempts to find the contract template using the provided contract template ID.
     * If the contract template exists, it retrieves the last template detail ID and then attempts
     * to find the corresponding contract template detail. If no such template detail is found,
     * a {@link DomainEntityNotFoundException} is thrown.
     *
     * @param contractTemplateId The ID of the contract template whose last template detail is to be retrieved.
     * @return The {@link ContractTemplateDetail} associated with the last template detail of the specified contract template.
     * @throws DomainEntityNotFoundException If no contract template or contract template detail is found for the given ID.
     */
    protected ContractTemplateDetail getContractTemplateLastDetails(Long contractTemplateId) {
        return Optional
                .ofNullable(getContractTemplate(contractTemplateId))
                .map(ContractTemplate::getLastTemplateDetailId)
                .flatMap(lastTemplateDetailId -> Optional
                        .of(contractTemplateDetailsRepository.findById(lastTemplateDetailId))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                )
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("Can't find template details for template id: %s".formatted(contractTemplateId))
                );
    }

    /**
     * Creates a {@link ByteArrayResource} from the template file associated with the given contract template detail.
     * This method fetches the template file path using the provided contract template detail, reads the file's content
     * as a byte array, and then wraps the byte array into a {@link ByteArrayResource} which can be used for further processing
     * such as file downloads or transmission.
     *
     * @param templateDetail The contract template detail from which the template file path is retrieved.
     * @return A {@link ByteArrayResource} containing the bytes of the template file.
     * @throws IOException If there is an issue reading the template file from the local file system.
     */
    protected ByteArrayResource createTemplateFileResource(ContractTemplateDetail templateDetail) throws IOException {
        Path templateFilePath = new File(documentGenerationUtil.getTemplateFileLocalPath(templateDetail)).toPath();
        byte[] fileBytes = Files.readAllBytes(templateFilePath);
        return new ByteArrayResource(fileBytes);
    }

    /**
     * Extracts the file suffix based on the provided date and contract template details.
     * This method delegates the extraction of the file suffix to the
     * {@link DocumentGenerationUtil#extractFileSuffix(LocalDateTime, ContractTemplateDetail)}
     * utility method, which uses the provided `LocalDateTime` and `ContractTemplateDetail`
     * to determine the appropriate suffix for the file.
     *
     * @param localDateTime          The date and time associated with the document generation.
     * @param contractTemplateDetail The details of the contract template used to generate the document.
     * @return The file suffix as a string, extracted based on the provided date and template details.
     */
    protected String extractFileSuffix(
            LocalDateTime localDateTime,
            ContractTemplateDetail contractTemplateDetail
    ) {
        return documentGenerationUtil.extractFileSuffix(localDateTime, contractTemplateDetail);
    }

    /**
     * Builds the destination path for a file based on the FTP base path, a folder path,
     * and the current date.
     * The destination path is constructed using the following format:
     * <pre>
     *     ftpBasePath/folderPath/LocalDate.now()
     * </pre>
     *
     * @return The constructed file destination path as a string.
     */
    protected String buildFileDestinationPath() {
        return String.format(
                "%s/%s/%s",
                ftpBasePath,
                folderPath(),
                LocalDate.now()
        );
    }

    /**
     * Converts the given amount to words in Bulgarian.
     * The amount is first rounded to two decimal places, and then the integer and fractional
     * parts are extracted. These parts are then passed to a utility method that transliterates
     * the values into Bulgarian words.
     *
     * @param amount The amount to be converted to words.
     * @return The amount represented in words, or null if an error occurs during the conversion.
     */
    protected String convertAmountToWords(BigDecimal amount) {
        try {
            BigDecimal totalWithScale2 = EPBDecimalUtils.roundToTwoDecimalPlaces(amount);
            BigDecimal integerAmount = totalWithScale2.setScale(0, RoundingMode.FLOOR);
            BigDecimal fractionAmount = totalWithScale2.subtract(totalWithScale2.setScale(0, RoundingMode.FLOOR)).movePointRight(totalWithScale2.scale());

            return BulgarianTransliterationUtil.convertAmountToWords(integerAmount.toBigInteger().intValue(), fractionAmount.intValue());
        } catch (Exception e) {
            log.error("Cannot transliterate amount in words", e);
        }
        return null;
    }

    /**
     * Saves a document to the repository with the provided details.
     *
     * @param fileName        the name of the document to be saved.
     * @param ftpPath         the path where the document will be stored. If null, the method returns null.
     * @param fileFormat      the format of the document file.
     * @param documentSigners a list of signers associated with the document.
     * @param templateId      the ID of the template associated with the document.
     * @return the saved {@link Document} object, or null if the ftpPath is null.
     */
    protected Document saveDocument(
            String fileName,
            String ftpPath,
            FileFormat fileFormat,
            List<DocumentSigners> documentSigners,
            Long templateId) {

        if (ftpPath == null) {
            return null;
        }

        Document document = Document.builder()
                .signers(documentSigners)
                .signedBy(new ArrayList<>())
                .name(fileName)
                .unsignedFileUrl(ftpPath)
                .signedFileUrl(ftpPath)
                .fileFormat(fileFormat)
                .templateId(templateId)
                .documentStatus(FileFormat.PDF.equals(fileFormat) ? DocumentStatus.UNSIGNED : DocumentStatus.SIGNED)
                .status(EntityStatus.ACTIVE)
                .build();

        return documentsRepository.saveAndFlush(document);
    }

    /**
     * Retrieves a list of {@link DocumentSigners} associated with a given contract template detail.
     *
     * @param templateDetail the contract template detail object containing information
     *                       about the file signing setup
     * @return a list of {@link DocumentSigners} extracted from the file signing details
     * in the provided template detail, or an empty list if no file signing information is present
     */
    protected List<DocumentSigners> getDocumentSigners(ContractTemplateDetail templateDetail) {
        return Optional
                .ofNullable(templateDetail.getFileSigning())
                .orElse(Collections.emptyList())
                .stream()
                .map(ContractTemplateSigning::getDocumentSigners)
                .toList();
    }

    /**
     * Retrieves the path of a folder where files are stored.
     *
     * @return the folder path as a string
     */
    protected abstract String folderPath();

    /**
     * Provides the FileService instance to interact with file operations.
     *
     * @return the FileService instance
     */
    protected FileService getFileService() {
        return fileService;
    }

}
