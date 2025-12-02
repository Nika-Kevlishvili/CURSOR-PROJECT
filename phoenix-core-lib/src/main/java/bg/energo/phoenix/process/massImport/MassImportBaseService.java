package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.Event;
import bg.energo.phoenix.event.EventFactory;
import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.process.model.request.ProcessCreatedEvent;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.process.service.ProcessService;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.kafka.RabbitMQProducerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.process.model.enums.ProcessStatus.NOT_STARTED;

@Slf4j
public abstract class MassImportBaseService {

    @Autowired
    protected FileService fileService;
    @Autowired
    protected PermissionService permissionService;
    @Autowired
    protected ProcessService processService;
    @Autowired
    protected TemplateRepository templateRepository;
    @Autowired
    protected EventFactory eventFactory;
    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    protected RabbitMQProducerService rabbitMQProducerService;

    @Value("${ftp.server.base.path}")
    String ftpBasePath;

    public abstract DomainType getDomainType();

    protected abstract EventType getEventType();

    protected abstract ProcessType getProcessType();

    protected abstract String getFileUploadPath();

    protected abstract PermissionContextEnum getPermissionContext();

    /**
     * Retrieves the mass import {@link Template} associated with the current {@link EventType} from the template repository,
     * and downloads it using the file service as a byte array.
     *
     * @return byte[]
     * @throws DomainEntityNotFoundException if no template was found.
     */
    public byte[] getMassImportTemplate() {
        try {
            var templatePath = templateRepository
                    .findById(getEventType().name())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find template path for mass import process"));
            return fileService.downloadFile(templatePath.getFileUrl()).getByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch mass import template", exception);
            throw new ClientException("Could not fetch mass import template", APPLICATION_ERROR);
        }
    }

    /**
     * Validates the file format and content for the given {@link MultipartFile} file, stores the file,
     * creates a process for handling the file, and publishes a {@link ProcessCreatedEvent}.
     *
     * @param file the mass import file to be uploaded
     * @throws ClientException if there is an error processing the uploaded mass import file
     */
    public void uploadMassImportFile(MultipartFile file, LocalDate date, Long collectionChannelId, Boolean currencyFromCollectionChannel) {
        validateFileFormat(file);
        validateFileContent(file, getMassImportTemplate());
        String remotePath = ftpBasePath + getFileUploadPath() + "/" + LocalDate.now();
        String fileUrl = fileService.uploadFile(file, remotePath, UUID.randomUUID() + "_" + file.getOriginalFilename());
        List<String> permissions = new ArrayList<>(permissionService.getPermissionsFromContext(getPermissionContext()));
        List<String> contractPermissions = permissionService.getPermissionsFromContext(PermissionContextEnum.PRODUCT_CONTRACTS);
        if (contractPermissions.contains(PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED.getId())) {
            permissions.add(PermissionEnum.PRODUCT_CONTRACT_EDIT_LOCKED.getId());
        }

        var process = processService.createProcess(
                getProcessType(),
                NOT_STARTED,
                fileUrl,
                String.join(";", permissions),
                date, null, null, currencyFromCollectionChannel
        );
        rabbitMQProducerService.publishProcessEvent(eventFactory.createProcessCreatedEvent(
                getEventType(),
                process
        ));
//        applicationEventPublisher.publishEvent(getProcessCreatedEvent(process));
    }

    /**
     * Validate the format of the given {@link MultipartFile} object.
     *
     * @param file the {@link MultipartFile} object to validate
     * @throws ClientException if the file is not in the expected Excel format.
     */
    private void validateFileFormat(MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            log.error("File has invalid format");
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    /**
     * Validates the content of the uploaded file against a predefined template.
     *
     * @param file                       The uploaded file to validate.
     * @param customerMassImportTemplate The predefined template to validate the file against.
     * @throws ClientException if there is an error while validating the file content.
     */
    private void validateFileContent(MultipartFile file, byte[] customerMassImportTemplate) {
        List<String> templateHeaders = getTemplateHeaders(customerMassImportTemplate);

        if (templateHeaders.isEmpty()) {
            log.error("Error happened while processing mass import template");
            throw new ClientException("Error happened while processing mass import template", APPLICATION_ERROR);
        }

        try (
                InputStream is = file.getInputStream();
                Workbook workbook = ExcelHelper.isXLS(file) ? new HSSFWorkbook(is) : new XSSFWorkbook(is)
        ) {
            Sheet firstSheet = workbook.getSheetAt(0);
            Row headerRow = firstSheet.getRow(0);

            int cellCount = headerRow.getPhysicalNumberOfCells();
            if (cellCount != templateHeaders.size()) {
                log.error("Cell count invalid in header");
                throw new ClientException("Invalid file format", APPLICATION_ERROR);
            }

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String currCell = headerRow.getCell(i).toString();
                if (!currCell.equals(templateHeaders.get(i))) {
                    log.error("Headers does not match");
                    throw new ClientException("Invalid file format", APPLICATION_ERROR);
                }
            }
        } catch (IOException e) {
            log.error("Error happened while validating mass import file content", e);
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    /**
     * Parses a byte array representing a mass import template,
     * and returns a list of the header names found in the first row of the template.
     *
     * @param massImportTemplate the byte array representing the mass import template
     * @return a list of header names found in the first row of the template
     * @throws ClientException if there was an error while processing the import template
     */
    private List<String> getTemplateHeaders(byte[] massImportTemplate) {
        List<String> templateHeaders = new ArrayList<>();

        try (
                InputStream is = new ByteArrayInputStream(massImportTemplate);
                Workbook templateWorkbook = new XSSFWorkbook(is)
        ) {
            Sheet firstSheet = templateWorkbook.getSheetAt(0);
            Row headerRow = firstSheet.getRow(0);
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String currCell = headerRow.getCell(i).toString();
                templateHeaders.add(currCell);
            }
        } catch (Exception e) {
            log.error("Error happened while processing mass import template");
            throw new ClientException("Error happened while processing mass import template", APPLICATION_ERROR);
        }

        return templateHeaders;
    }

    public Event getProcessCreatedEvent(Process process) {
        return eventFactory.createProcessCreatedEvent(getEventType(), process);
    }

}
